#!/usr/bin/env node
/**
 * ClipHist Android MCP Server
 *
 * Exposes the Android clipboard history to Claude Desktop and Claude Code
 * via the Model Context Protocol (MCP).
 *
 * Setup:
 *   1. Start LocalApiService on Android (enable in app Settings → Local API)
 *   2. Forward the port via ADB:  adb forward tcp:8765 tcp:8765
 *   3. Add this server to claude_desktop_config.json:
 *
 *      {
 *        "mcpServers": {
 *          "cliphist-android": {
 *            "command": "node",
 *            "args": ["/path/to/mcp-server/dist/index.js"],
 *            "env": {
 *              "CLIPHIST_API_URL": "http://localhost:8765",
 *              "CLIPHIST_API_TOKEN": "<your-token>"
 *            }
 *          }
 *        }
 *      }
 */

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  Tool,
} from "@modelcontextprotocol/sdk/types.js";

// ── Configuration ────────────────────────────────────────────────────────────

const API_URL = process.env.CLIPHIST_API_URL ?? "http://localhost:8765";
const API_TOKEN = process.env.CLIPHIST_API_TOKEN ?? "";

// ── HTTP helper ───────────────────────────────────────────────────────────────

async function apiRequest(
  path: string,
  method = "GET",
  body?: unknown
): Promise<unknown> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (API_TOKEN) {
    headers["Authorization"] = `Bearer ${API_TOKEN}`;
  }

  const response = await fetch(`${API_URL}${path}`, {
    method,
    headers,
    body: body != null ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`ClipHist API error ${response.status}: ${text}`);
  }

  return response.json();
}

// ── Tool definitions ──────────────────────────────────────────────────────────

const TOOLS: Tool[] = [
  {
    name: "get_clipboard_history",
    description:
      "Retrieve the Android clipboard history. Returns recent clipboard items with their content, type and timestamp.",
    inputSchema: {
      type: "object",
      properties: {
        limit: {
          type: "number",
          description: "Maximum number of items to return (default 20, max 100)",
        },
      },
    },
  },
  {
    name: "search_clipboard",
    description:
      "Full-text search across the Android clipboard history. Returns matching items.",
    inputSchema: {
      type: "object",
      properties: {
        query: {
          type: "string",
          description: "Search query string",
        },
      },
      required: ["query"],
    },
  },
  {
    name: "get_clipboard_item",
    description: "Retrieve a single clipboard item by its ID.",
    inputSchema: {
      type: "object",
      properties: {
        id: {
          type: "string",
          description: "The clipboard item ID",
        },
      },
      required: ["id"],
    },
  },
  {
    name: "add_to_clipboard",
    description:
      "Add a new text item to the Android clipboard history. The item will appear in the app and on the device clipboard.",
    inputSchema: {
      type: "object",
      properties: {
        content: {
          type: "string",
          description: "The text content to add",
        },
      },
      required: ["content"],
    },
  },
  {
    name: "get_clipboard_stats",
    description:
      "Get high-level statistics about the Android clipboard history (total items, favorites, activity).",
    inputSchema: {
      type: "object",
      properties: {},
    },
  },
];

// ── Tool handlers ─────────────────────────────────────────────────────────────

async function handleGetClipboardHistory(args: {
  limit?: number;
}): Promise<string> {
  const limit = Math.min(args.limit ?? 20, 100);
  const data = (await apiRequest(`/items?limit=${limit}`)) as {
    items: ClipboardItemJson[];
    count: number;
  };
  if (data.items.length === 0) {
    return "Clipboard history is empty.";
  }
  const lines = data.items.map(
    (item, i) =>
      `[${i + 1}] ID: ${item.id}\n` +
      `    Type: ${item.contentType}\n` +
      `    Time: ${new Date(item.timestamp).toISOString()}\n` +
      `    Content: ${item.content.length > 200 ? item.content.slice(0, 200) + "…" : item.content}`
  );
  return `Clipboard history (${data.count} items shown):\n\n${lines.join("\n\n")}`;
}

async function handleSearchClipboard(args: {
  query: string;
}): Promise<string> {
  const data = (await apiRequest(
    `/search?q=${encodeURIComponent(args.query)}`
  )) as { items: ClipboardItemJson[]; count: number };
  if (data.items.length === 0) {
    return `No clipboard items found matching "${args.query}".`;
  }
  const lines = data.items.map(
    (item, i) =>
      `[${i + 1}] ID: ${item.id}\n` +
      `    Type: ${item.contentType}\n` +
      `    Time: ${new Date(item.timestamp).toISOString()}\n` +
      `    Content: ${item.content.length > 300 ? item.content.slice(0, 300) + "…" : item.content}`
  );
  return `Search results for "${args.query}" (${data.count} matches):\n\n${lines.join("\n\n")}`;
}

async function handleGetClipboardItem(args: { id: string }): Promise<string> {
  const item = (await apiRequest(`/items/${args.id}`)) as ClipboardItemJson;
  return (
    `ID: ${item.id}\n` +
    `Type: ${item.contentType}\n` +
    `Time: ${new Date(item.timestamp).toISOString()}\n` +
    `Favorite: ${item.isFavorite}\n` +
    `Size: ${item.size} bytes\n` +
    `Content:\n${item.content}`
  );
}

async function handleAddToClipboard(args: {
  content: string;
}): Promise<string> {
  const result = (await apiRequest("/items", "POST", {
    content: args.content,
  })) as { id: string; created: boolean };
  return `Added to clipboard history. Item ID: ${result.id}`;
}

async function handleGetClipboardStats(): Promise<string> {
  const stats = (await apiRequest("/stats")) as ClipboardStatsJson;
  return (
    `Clipboard Statistics:\n` +
    `  Total items: ${stats.totalItems}\n` +
    `  Favorites: ${stats.favoriteItems}\n` +
    `  Added today: ${stats.itemsToday}\n` +
    `  Added this week: ${stats.itemsThisWeek}\n` +
    `  Most used type: ${stats.mostUsedContentType}\n` +
    `  Average content length: ${stats.averageContentLength} chars\n` +
    `  Last activity: ${new Date(stats.lastActivityTimestamp).toISOString()}`
  );
}

// ── Types ─────────────────────────────────────────────────────────────────────

interface ClipboardItemJson {
  id: string;
  content: string;
  timestamp: number;
  contentType: string;
  size: number;
  isFavorite: boolean;
}

interface ClipboardStatsJson {
  totalItems: number;
  favoriteItems: number;
  itemsToday: number;
  itemsThisWeek: number;
  mostUsedContentType: string;
  averageContentLength: number;
  lastActivityTimestamp: number;
}

// ── Server setup ──────────────────────────────────────────────────────────────

async function main() {
  const server = new Server(
    { name: "cliphist-android", version: "1.0.0" },
    { capabilities: { tools: {} } }
  );

  server.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools: TOOLS,
  }));

  server.setRequestHandler(CallToolRequestSchema, async (request) => {
    const { name, arguments: args = {} } = request.params;

    try {
      let text: string;
      switch (name) {
        case "get_clipboard_history":
          text = await handleGetClipboardHistory(args as { limit?: number });
          break;
        case "search_clipboard":
          text = await handleSearchClipboard(args as { query: string });
          break;
        case "get_clipboard_item":
          text = await handleGetClipboardItem(args as { id: string });
          break;
        case "add_to_clipboard":
          text = await handleAddToClipboard(args as { content: string });
          break;
        case "get_clipboard_stats":
          text = await handleGetClipboardStats();
          break;
        default:
          throw new Error(`Unknown tool: ${name}`);
      }
      return { content: [{ type: "text", text }] };
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      return {
        content: [{ type: "text", text: `Error: ${message}` }],
        isError: true,
      };
    }
  });

  const transport = new StdioServerTransport();
  await server.connect(transport);
  process.stderr.write(`ClipHist MCP server running (API: ${API_URL})\n`);
}

main().catch((err) => {
  process.stderr.write(`Fatal: ${err}\n`);
  process.exit(1);
});

/**
 * ClipHist Android - VS Code / Cursor Extension
 *
 * Brings your Android clipboard history into VS Code and Cursor.
 *
 * Usage:
 *   1. Enable Local API in ClipHist Android app (Settings → Local API)
 *   2. Run: adb forward tcp:8765 tcp:8765
 *   3. Install this extension and set your API token in settings
 *   4. Press Ctrl+Shift+V (Cmd+Shift+V on Mac) to paste from history
 */

import * as vscode from "vscode";

interface ClipboardItem {
  id: string;
  content: string;
  timestamp: number;
  contentType: string;
  size: number;
  isFavorite: boolean;
}

// ── API client ────────────────────────────────────────────────────────────────

async function fetchApi<T>(
  path: string,
  method = "GET",
  body?: unknown
): Promise<T> {
  const config = vscode.workspace.getConfiguration("cliphist");
  const apiUrl = config.get<string>("apiUrl", "http://localhost:8765");
  const apiToken = config.get<string>("apiToken", "");

  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (apiToken) {
    headers["Authorization"] = `Bearer ${apiToken}`;
  }

  let res: Response;
  try {
    res = await fetch(`${apiUrl}${path}`, {
      method,
      headers,
      body: body != null ? JSON.stringify(body) : undefined,
    });
  } catch (e) {
    throw new Error(
      `Cannot reach ClipHist Android API at ${apiUrl}.\n` +
        "Make sure the Local API is enabled in the app and ADB forwarding is active:\n" +
        "  adb forward tcp:8765 tcp:8765"
    );
  }

  if (!res.ok) {
    const text = await res.text();
    throw new Error(`ClipHist API ${res.status}: ${text}`);
  }

  return res.json() as Promise<T>;
}

// ── QuickPick helper ──────────────────────────────────────────────────────────

function itemLabel(item: ClipboardItem): string {
  const preview = item.content.replace(/\s+/g, " ").trim();
  return preview.length > 80 ? preview.slice(0, 80) + "…" : preview;
}

function itemDetail(item: ClipboardItem): string {
  const date = new Date(item.timestamp).toLocaleString();
  return `${item.contentType}  ·  ${item.size} bytes  ·  ${date}${item.isFavorite ? "  ★" : ""}`;
}

async function pickItem(title: string, items: ClipboardItem[]): Promise<ClipboardItem | undefined> {
  const picks = items.map((item) => ({
    label: itemLabel(item),
    description: item.contentType,
    detail: itemDetail(item),
    item,
  }));

  const selected = await vscode.window.showQuickPick(picks, {
    title,
    matchOnDetail: true,
    matchOnDescription: true,
  });

  return selected?.item;
}

// ── Commands ──────────────────────────────────────────────────────────────────

async function cmdPasteFromHistory() {
  const config = vscode.workspace.getConfiguration("cliphist");
  const limit = config.get<number>("maxItems", 30);

  const data = await fetchApi<{ items: ClipboardItem[]; count: number }>(
    `/items?limit=${limit}`
  );

  if (data.items.length === 0) {
    vscode.window.showInformationMessage("ClipHist: No clipboard history found.");
    return;
  }

  const selected = await pickItem("Paste from Android Clipboard History", data.items);
  if (!selected) return;

  const editor = vscode.window.activeTextEditor;
  if (!editor) {
    // No editor — copy to system clipboard instead
    await vscode.env.clipboard.writeText(selected.content);
    vscode.window.showInformationMessage("Copied to clipboard.");
    return;
  }

  editor.edit((edit) => {
    for (const sel of editor.selections) {
      edit.replace(sel, selected.content);
    }
  });
}

async function cmdSearch() {
  const query = await vscode.window.showInputBox({
    prompt: "Search Android clipboard history",
    placeHolder: "Enter search query…",
  });
  if (!query) return;

  const data = await fetchApi<{ items: ClipboardItem[]; count: number }>(
    `/search?q=${encodeURIComponent(query)}`
  );

  if (data.items.length === 0) {
    vscode.window.showInformationMessage(`ClipHist: No results for "${query}".`);
    return;
  }

  const selected = await pickItem(
    `Search results for "${query}" (${data.count} found)`,
    data.items
  );
  if (!selected) return;

  const editor = vscode.window.activeTextEditor;
  if (!editor) {
    await vscode.env.clipboard.writeText(selected.content);
    vscode.window.showInformationMessage("Copied to clipboard.");
    return;
  }

  editor.edit((edit) => {
    for (const sel of editor.selections) {
      edit.replace(sel, selected.content);
    }
  });
}

async function cmdShowRecent() {
  const config = vscode.workspace.getConfiguration("cliphist");
  const limit = config.get<number>("maxItems", 30);
  const data = await fetchApi<{ items: ClipboardItem[]; count: number }>(
    `/items?limit=${limit}`
  );

  if (data.items.length === 0) {
    vscode.window.showInformationMessage("ClipHist: No clipboard history found.");
    return;
  }

  const selected = await pickItem(
    `Recent Android Clipboard Items (${data.count} shown)`,
    data.items
  );
  if (!selected) return;

  await vscode.env.clipboard.writeText(selected.content);
  vscode.window.showInformationMessage("Copied to system clipboard.");
}

// ── Sidebar TreeView ──────────────────────────────────────────────────────────

class ClipHistTreeProvider implements vscode.TreeDataProvider<ClipboardItem> {
  private _onDidChangeTreeData = new vscode.EventEmitter<ClipboardItem | undefined>();
  readonly onDidChangeTreeData = this._onDidChangeTreeData.event;

  refresh() {
    this._onDidChangeTreeData.fire(undefined);
  }

  async getChildren(): Promise<ClipboardItem[]> {
    try {
      const data = await fetchApi<{ items: ClipboardItem[] }>("/items?limit=20");
      return data.items;
    } catch {
      return [];
    }
  }

  getTreeItem(item: ClipboardItem): vscode.TreeItem {
    const treeItem = new vscode.TreeItem(itemLabel(item), vscode.TreeItemCollapsibleState.None);
    treeItem.tooltip = item.content;
    treeItem.description = item.contentType;
    treeItem.command = {
      command: "cliphist.pasteFromHistory",
      title: "Paste",
    };
    return treeItem;
  }
}

// ── Activation ────────────────────────────────────────────────────────────────

export function activate(context: vscode.ExtensionContext) {
  const treeProvider = new ClipHistTreeProvider();
  vscode.window.registerTreeDataProvider("cliphist.sidebarView", treeProvider);

  const withErrorHandling =
    (fn: () => Promise<void>) => async () => {
      try {
        await fn();
      } catch (e) {
        vscode.window.showErrorMessage(
          `ClipHist Error: ${e instanceof Error ? e.message : String(e)}`
        );
      }
    };

  context.subscriptions.push(
    vscode.commands.registerCommand(
      "cliphist.pasteFromHistory",
      withErrorHandling(cmdPasteFromHistory)
    ),
    vscode.commands.registerCommand(
      "cliphist.search",
      withErrorHandling(cmdSearch)
    ),
    vscode.commands.registerCommand(
      "cliphist.showRecent",
      withErrorHandling(cmdShowRecent)
    )
  );
}

export function deactivate() {}

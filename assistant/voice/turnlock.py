"""One voice turn at a time, across processes: the HTTP trigger (http_trigger.py) and the
MCP tools `speak` / `listen` (assistant/mcp/server.py) take the same flock on
state/turn.lock (override with VA_LOCK_FILE). flock is per open file, so two threads of
one process that each call turn_lock() also exclude each other.
"""
from __future__ import annotations

import fcntl
import os
import time
from contextlib import contextmanager

HERE = os.path.dirname(os.path.abspath(__file__))


def lock_path() -> str:
    return os.environ.get("VA_LOCK_FILE") or os.path.join(HERE, "state", "turn.lock")


@contextmanager
def turn_lock(timeout: float = 0):
    """Yield True once the lock is held, or False if it is still busy after `timeout` s."""
    path = lock_path()
    os.makedirs(os.path.dirname(path), exist_ok=True)
    fd = os.open(path, os.O_RDWR | os.O_CREAT, 0o600)
    deadline = time.monotonic() + timeout
    got = False
    try:
        while True:
            try:
                fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
                got = True
                break
            except BlockingIOError:
                if time.monotonic() >= deadline:
                    break
                time.sleep(0.2)
        yield got
    finally:
        if got:
            fcntl.flock(fd, fcntl.LOCK_UN)
        os.close(fd)

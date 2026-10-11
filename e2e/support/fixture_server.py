#!/usr/bin/env python3
"""Loopback-only Kakao-shaped fixture. No credentials or outbound requests."""
import argparse
import json
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

LOCK = threading.Lock()
REQUESTS = []
EVENTS = []
FAILURES = set()
DELAYS = {}
LIMIT = 100


def book(index, prefix="Fixture"):
    return dict(title=f"{prefix} Book {index:02d}", contents="Fixture description",
                url=f"https://example.invalid/books/{index}", isbn=f"978000000{index:04d}",
                datetime=f"2026-09-{(index % 28) + 1:02d}T00:00:00.000+09:00",
                authors=["Fixture Author"], publisher="Fixture Publisher", translators=[],
                price=10000 * index if index != 3 else 0,
                sale_price=9000 * index if index != 3 else -1,
                thumbnail="", status="정상판매")


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def respond(self, value, status=200):
        data = json.dumps(value, ensure_ascii=False).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        try:
            self.wfile.write(data)
        except (BrokenPipeError, ConnectionResetError):
            pass  # A cancelled old query must not turn the fixture into a failure loop.
        if hasattr(self, "trace"):
            self.trace["completed"] = time.monotonic()

    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path == "/__requests":
            with LOCK:
                self.respond(list(REQUESTS))
            return
        if parsed.path == "/__events":
            with LOCK:
                self.respond(list(EVENTS))
            return
        if parsed.path == "/__health":
            self.respond({"fixture": True})
            return
        if parsed.path != "/v3/search/book":
            self.respond({"error": "unexpected path"}, 404)
            return
        params = parse_qs(parsed.query, keep_blank_values=True)
        try:
            query, sort = params["query"][0], params["sort"][0]
            page, size = int(params["page"][0]), int(params["size"][0])
            assert size == 20 and 1 <= page <= 50 and sort in ("accuracy", "latest")
            assert self.headers.get("Authorization") == "KakaoAK fixture-only"
        except (KeyError, ValueError, AssertionError):
            self.respond({"error": "request contract violation"}, 400)
            return
        with LOCK:
            REQUESTS.append(dict(query=query, sort=sort, page=page, size=size))
            self.trace = dict(query=query, page=page, started=time.monotonic(), completed=None)
            EVENTS.append(self.trace)
            failed = query in FAILURES or f"{query}:{page}" in FAILURES
            over_limit = len(REQUESTS) > LIMIT
            delay = DELAYS.get(query, 0)
        if delay:
            time.sleep(delay)
        if over_limit or failed or query == "servererror":
            self.respond({"error": "fixture failure"}, 503)
            return
        if query == "empty":
            documents, total, end = [], 0, True
        elif query == "missing":
            documents = [dict(title="Missing Book", url="https://example.invalid/missing")]
            total, end = 1, True
        elif query == "pages":
            indices = [20] + list(range(22, 41)) if page == 2 else list(range(1, 21))
            documents = [book(i, "Page") for i in indices]
            total, end = 40, page >= 2
        else:
            # ISBN queries deliberately return no API result: detail snapshot fallback must work.
            prefix = "Old" if query == "slow" else "New" if query == "new" else "Fixture"
            documents = [] if query.isdigit() else [book(i, prefix) for i in (1, 2, 3)]
            if sort == "latest":
                documents.reverse()
            total, end = len(documents), True
        self.respond(dict(meta=dict(total_count=total, pageable_count=total, is_end=end), documents=documents))

    def do_POST(self):
        length = int(self.headers.get("Content-Length", "0"))
        if length > 2048:
            self.respond({}, 413)
            return
        data = json.loads(self.rfile.read(length) or "{}")
        with LOCK:
            if self.path == "/__reset":
                REQUESTS.clear()
                EVENTS.clear()
                FAILURES.clear()
                DELAYS.clear()
            elif self.path == "/__fail":
                FAILURES.clear()
                FAILURES.update(data.get("queries", []))
            elif self.path == "/__delay":
                DELAYS.clear()
                DELAYS.update({k: max(0, min(float(v), 14)) for k, v in data.get("queries", {}).items()})
            else:
                self.respond({}, 404)
                return
        self.respond({"ok": True})


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8877)
    args = parser.parse_args()
    print(f"Fixture listening on 127.0.0.1:{args.port}", flush=True)
    ThreadingHTTPServer(("127.0.0.1", args.port), Handler).serve_forever()

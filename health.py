#!/usr/bin/env python3
"""Minimal HTTP health endpoint."""

import argparse
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


class HealthHandler(BaseHTTPRequestHandler):
    """Serve a machine-readable liveness response."""

    def do_GET(self):  # noqa: N802 - required by BaseHTTPRequestHandler
        if self.path != "/health":
            self.send_error(404, "Not Found")
            return

        payload = json.dumps({"status": "ok"}).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def log_message(self, *_args):
        """Keep the lightweight endpoint quiet by default."""


def main():
    parser = argparse.ArgumentParser(description="Run the health endpoint")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8080)
    args = parser.parse_args()

    server = ThreadingHTTPServer((args.host, args.port), HealthHandler)
    print(f"Health endpoint listening on http://{args.host}:{args.port}/health")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()

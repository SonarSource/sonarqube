#!/usr/bin/env python3
"""Minimal HTTP health endpoint for local service checks."""

import argparse
import json
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


class HealthHandler(BaseHTTPRequestHandler):
    """Serve the service health status."""

    def do_GET(self):  # noqa: N802 - required by BaseHTTPRequestHandler
        if self.path != "/health":
            self.send_error(HTTPStatus.NOT_FOUND)
            return

        body = json.dumps({"status": "ok"}).encode("utf-8")
        self.send_response(HTTPStatus.OK)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format, *args):
        """Keep the endpoint quiet by default."""


def main():
    parser = argparse.ArgumentParser(description="Run the health status endpoint")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8080)
    args = parser.parse_args()

    with ThreadingHTTPServer((args.host, args.port), HealthHandler) as server:
        server.serve_forever()


if __name__ == "__main__":
    main()

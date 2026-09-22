import json
import unittest
from http.client import HTTPConnection
from threading import Thread
from http.server import ThreadingHTTPServer

from health import HealthHandler


class HealthEndpointTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = ThreadingHTTPServer(("127.0.0.1", 0), HealthHandler)
        cls.thread = Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join()

    def request(self, path):
        connection = HTTPConnection(*self.server.server_address)
        connection.request("GET", path)
        response = connection.getresponse()
        body = response.read()
        connection.close()
        return response, body

    def test_health_returns_ok_json(self):
        response, body = self.request("/health")

        self.assertEqual(response.status, 200)
        self.assertEqual(response.getheader("Content-Type"), "application/json")
        self.assertEqual(json.loads(body), {"status": "ok"})

    def test_unknown_path_returns_not_found(self):
        response, _ = self.request("/not-health")

        self.assertEqual(response.status, 404)


if __name__ == "__main__":
    unittest.main()

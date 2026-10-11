import json
import threading
import unittest
import urllib.error
import urllib.request

import fixture_server as fixture


class FixtureContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = fixture.ThreadingHTTPServer(("127.0.0.1", 0), fixture.Handler)
        cls.url = f"http://127.0.0.1:{cls.server.server_port}"
        threading.Thread(target=cls.server.serve_forever, daemon=True).start()

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()

    def setUp(self):
        with fixture.LOCK:
            fixture.REQUESTS.clear()
            fixture.EVENTS.clear()
            fixture.FAILURES.clear()
            fixture.DELAYS.clear()

    def request(self, path, token="fixture-only"):
        request = urllib.request.Request(self.url + path, headers={"Authorization": f"KakaoAK {token}"})
        with urllib.request.urlopen(request, timeout=2) as response:
            return json.load(response)

    def test_terminal_page_has_twenty_items(self):
        first = self.request("/v3/search/book?query=pages&sort=accuracy&page=1&size=20")
        last = self.request("/v3/search/book?query=pages&sort=accuracy&page=2&size=20")
        self.assertEqual(len(first["documents"]), 20)
        self.assertFalse(first["meta"]["is_end"])
        self.assertEqual(len(last["documents"]), 20)
        self.assertTrue(last["meta"]["is_end"])

    def test_contract_rejects_real_token_and_wrong_size(self):
        for size, token in [(20, "real-token"), (19, "fixture-only")]:
            with self.assertRaises(urllib.error.HTTPError) as failure:
                self.request(f"/v3/search/book?query=fixture&sort=accuracy&page=1&size={size}", token)
            self.assertEqual(failure.exception.code, 400)
        self.assertEqual(fixture.REQUESTS, [])

    def test_failure_can_be_removed_for_explicit_retry(self):
        with fixture.LOCK:
            fixture.FAILURES.add("fixture")
        with self.assertRaises(urllib.error.HTTPError) as failure:
            self.request("/v3/search/book?query=fixture&sort=accuracy&page=1&size=20")
        self.assertEqual(failure.exception.code, 503)
        with fixture.LOCK:
            fixture.FAILURES.clear()
        recovered = self.request("/v3/search/book?query=fixture&sort=accuracy&page=1&size=20")
        self.assertEqual(len(recovered["documents"]), 3)
        self.assertEqual(len(fixture.REQUESTS), 2)


if __name__ == "__main__":
    unittest.main()

# Lightweight health endpoint

Run the dependency-free endpoint with:

```bash
python3 health.py
```

It serves `GET /health` on `127.0.0.1:8080` and returns `{"status": "ok"}`.
Use `--host` and `--port` to configure the bind address.

Run its tests with:

```bash
python3 -m unittest test_health.py
```

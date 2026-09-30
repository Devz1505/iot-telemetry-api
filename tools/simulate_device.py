"""Pretend to be an ESP32 with a DHT22 so you can demo the API without hardware.

Registers a device, sets a temperature threshold, then posts a temperature +
humidity batch every few seconds. Now and then it injects a heat spike so an
alert fires and shows up on the dashboard.

Standard library only:  python tools/simulate_device.py
Options:                python tools/simulate_device.py --help
"""

import argparse
import json
import math
import random
import time
import urllib.error
import urllib.request


def call(base_url, method, path, body=None):
    """Send a JSON request; return (status, parsed JSON body or None)."""
    data = json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request(base_url + path, data=data, method=method,
                                     headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(request, timeout=5) as response:
            raw = response.read()
            return response.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as error:
        return error.code, json.loads(error.read() or b"{}")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--device-id", default="sim-esp32-01")
    parser.add_argument("--interval", type=float, default=3.0, help="seconds between batches")
    parser.add_argument("--count", type=int, default=0, help="batches to send (0 = forever)")
    parser.add_argument("--max-temp", type=float, default=32.0, help="alert threshold in degrees C")
    args = parser.parse_args()

    status, body = call(args.base_url, "POST", "/api/devices",
                        {"deviceId": args.device_id, "name": "Simulated DHT22 node", "location": "Laptop"})
    if status not in (201, 409):  # 409 = already registered, which is fine
        raise SystemExit(f"Could not register device: {status} {body}")

    call(args.base_url, "PUT", f"/api/devices/{args.device_id}/thresholds/TEMPERATURE", {"max": args.max_temp})
    print(f"Sending as '{args.device_id}' to {args.base_url} (Ctrl+C to stop)")

    sent = 0
    while args.count == 0 or sent < args.count:
        # A slow daily-style wave plus sensor noise; ~5% of batches get a heat spike.
        t = time.time() / 60
        temperature = 27 + 3 * math.sin(t) + random.gauss(0, 0.3)
        if random.random() < 0.05:
            temperature += 8
        humidity = 60 - 8 * math.sin(t) + random.gauss(0, 1.0)

        batch = {"readings": [
            {"sensorType": "TEMPERATURE", "value": round(temperature, 2)},
            {"sensorType": "HUMIDITY", "value": round(min(max(humidity, 0), 100), 1)},
        ]}
        status, body = call(args.base_url, "POST", f"/api/devices/{args.device_id}/readings", batch)
        note = f"  <- {body['alertsRaised']} alert(s)" if status == 201 and body["alertsRaised"] else ""
        print(f"[{status}] T={temperature:5.2f} C  RH={humidity:4.1f} %{note}")

        sent += 1
        time.sleep(args.interval)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nStopped.")
    except urllib.error.URLError as error:
        raise SystemExit(f"API not reachable ({error.reason}). Is the server running?")

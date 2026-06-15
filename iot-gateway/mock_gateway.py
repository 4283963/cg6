import json
import time
import random
import threading
import paho.mqtt.client as mqtt


class MockGateway:
    MOCK_UNDERPASSES = [
        {"underpass_id": "UP-BRIDGE-01", "sensor_id": "DEPTH-001"},
        {"underpass_id": "UP-BRIDGE-02", "sensor_id": "DEPTH-002"},
        {"underpass_id": "UP-BRIDGE-03", "sensor_id": "DEPTH-003"},
        {"underpass_id": "UP-BRIDGE-04", "sensor_id": "DEPTH-004"},
        {"underpass_id": "UP-BRIDGE-05", "sensor_id": "DEPTH-005"},
    ]

    def __init__(self, broker="127.0.0.1", port=1883):
        self.client = mqtt.Client(client_id="mock-gateway-" + str(int(time.time())))
        self.broker = broker
        self.port = port
        self._running = False
        self._depths = {u["underpass_id"]: random.uniform(20, 80) for u in self.MOCK_UNDERPASSES}

        self.client.on_connect = self._on_connect
        self.client.on_message = self._on_message

    def _on_connect(self, client, userdata, flags, rc):
        print(f"[Mock] Connected to broker, rc={rc}")
        client.subscribe("underpass/command/+/hydraulic", qos=1)

    def _on_message(self, client, userdata, msg):
        payload = json.loads(msg.payload.decode())
        print(f"[Mock] Received hydraulic command: {msg.topic} -> {payload}")

    def _simulate_depth(self, underpass_id):
        current = self._depths[underpass_id]
        delta = random.uniform(-15, 20)
        if current > 150:
            delta = random.uniform(-30, -5)
        elif current > 100:
            delta = random.uniform(-20, 10)
        new_depth = max(0, min(300, current + delta))
        self._depths[underpass_id] = new_depth
        return round(new_depth, 1)

    def _publish_loop(self):
        while self._running:
            for u in self.MOCK_UNDERPASSES:
                depth_mm = self._simulate_depth(u["underpass_id"])
                payload = {
                    "underpassId": u["underpass_id"],
                    "sensorId": u["sensor_id"],
                    "depthMm": depth_mm,
                    "timestamp": int(time.time() * 1000)
                }
                self.client.publish("underpass/sensor/data", json.dumps(payload), qos=1)
                status = "⚠ ALARM" if depth_mm >= 100 else "  normal"
                print(f"[Mock] {u['underpass_id']}: {depth_mm:.1f}mm  {status}")
            time.sleep(3)

    def start(self):
        print("[Mock] Starting mock IoT gateway...")
        self.client.connect(self.broker, self.port, 60)
        self.client.loop_start()
        self._running = True
        self._thread = threading.Thread(target=self._publish_loop, daemon=True)
        self._thread.start()
        print("[Mock] Gateway running, simulating sensor data every 3s")

        try:
            while self._running:
                time.sleep(1)
        except KeyboardInterrupt:
            self.stop()

    def stop(self):
        self._running = False
        self.client.loop_stop()
        self.client.disconnect()
        print("[Mock] Stopped")


if __name__ == "__main__":
    gateway = MockGateway()
    gateway.start()

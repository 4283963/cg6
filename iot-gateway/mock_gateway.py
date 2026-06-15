import json
import time
import random
import threading
import paho.mqtt.client as mqtt


class MockGateway:
    MOCK_UNDERPASSES = [
        {"underpass_id": "UP-BRIDGE-01", "sensor_id": "DEPTH-001", "rain_sensor_id": "RAIN-001"},
        {"underpass_id": "UP-BRIDGE-02", "sensor_id": "DEPTH-002", "rain_sensor_id": "RAIN-002"},
        {"underpass_id": "UP-BRIDGE-03", "sensor_id": "DEPTH-003", "rain_sensor_id": "RAIN-003"},
        {"underpass_id": "UP-BRIDGE-04", "sensor_id": "DEPTH-004", "rain_sensor_id": "RAIN-004"},
        {"underpass_id": "UP-BRIDGE-05", "sensor_id": "DEPTH-005", "rain_sensor_id": "RAIN-005"},
    ]

    MOCK_CATCHMENTS = [
        {"catchment_id": "CATCH-001", "flow_meter_id": "FM-001"},
        {"catchment_id": "CATCH-002", "flow_meter_id": "FM-002"},
        {"catchment_id": "CATCH-003", "flow_meter_id": "FM-003"},
        {"catchment_id": "CATCH-004", "flow_meter_id": "FM-004"},
        {"catchment_id": "CATCH-005", "flow_meter_id": "FM-005"},
    ]

    def __init__(self, broker="127.0.0.1", port=1883):
        self.client = mqtt.Client(client_id="mock-gateway-" + str(int(time.time())))
        self.broker = broker
        self.port = port
        self._running = False
        self._depths = {u["underpass_id"]: random.uniform(20, 80) for u in self.MOCK_UNDERPASSES}
        self._flows = {c["catchment_id"]: random.uniform(5, 15) for c in self.MOCK_CATCHMENTS}
        self._raining = {u["underpass_id"]: False for u in self.MOCK_UNDERPASSES}
        self._rain_mm = {u["underpass_id"]: 0.0 for u in self.MOCK_UNDERPASSES}
        self._surge_mode = {c["catchment_id"]: False for c in self.MOCK_CATCHMENTS}

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

    def _simulate_flow(self, catchment_id):
        current = self._flows[catchment_id]
        if self._surge_mode[catchment_id]:
            delta = random.uniform(10, 25)
            if current > 100:
                self._surge_mode[catchment_id] = False
        else:
            delta = random.uniform(-3, 4)
            if random.random() < 0.03:
                self._surge_mode[catchment_id] = True
                print(f"[Mock] ⚡ Flow surge triggered for {catchment_id}")
        new_flow = max(2, min(200, current + delta))
        self._flows[catchment_id] = new_flow
        return round(new_flow, 2)

    def _simulate_rainfall(self, underpass_id):
        if random.random() < 0.02:
            self._raining[underpass_id] = not self._raining[underpass_id]
        if self._raining[underpass_id]:
            self._rain_mm[underpass_id] = round(random.uniform(0.5, 8.0), 2)
        else:
            self._rain_mm[underpass_id] = 0.0
        return self._raining[underpass_id], self._rain_mm[underpass_id]

    def _publish_depth_loop(self):
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
                print(f"[Mock][Depth] {u['underpass_id']}: {depth_mm:.1f}mm  {status}")
            time.sleep(3)

    def _publish_flow_loop(self):
        while self._running:
            for c in self.MOCK_CATCHMENTS:
                flow_lps = self._simulate_flow(c["catchment_id"])
                payload = {
                    "catchmentId": c["catchment_id"],
                    "flowMeterId": c["flow_meter_id"],
                    "flowRateLps": flow_lps,
                    "timestamp": int(time.time() * 1000)
                }
                self.client.publish("underpass/upstream/flow", json.dumps(payload), qos=1)
                surge = "  SURGE" if self._surge_mode[c["catchment_id"]] else ""
                print(f"[Mock][Flow ] {c['catchment_id']}: {flow_lps:.1f} L/s{surge}")
            time.sleep(5)

    def _publish_rainfall_loop(self):
        while self._running:
            for u in self.MOCK_UNDERPASSES:
                raining, mm = self._simulate_rainfall(u["underpass_id"])
                payload = {
                    "underpassId": u["underpass_id"],
                    "sensorId": u["rain_sensor_id"],
                    "raining": raining,
                    "rainMmPerHour": mm,
                    "timestamp": int(time.time() * 1000)
                }
                self.client.publish("underpass/sensor/rainfall", json.dumps(payload), qos=1)
                weather = "🌧" if raining else "☀"
                print(f"[Mock][Rain ] {u['underpass_id']}: {weather} {mm:.1f} mm/h")
            time.sleep(6)

    def start(self):
        print("[Mock] Starting mock IoT gateway...")
        self.client.connect(self.broker, self.port, 60)
        self.client.loop_start()
        self._running = True
        self._t_depth = threading.Thread(target=self._publish_depth_loop, daemon=True)
        self._t_flow = threading.Thread(target=self._publish_flow_loop, daemon=True)
        self._t_rain = threading.Thread(target=self._publish_rainfall_loop, daemon=True)
        self._t_depth.start()
        self._t_flow.start()
        self._t_rain.start()
        print("[Mock] Gateway running (depth: 3s, flow: 5s, rain: 6s)")

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

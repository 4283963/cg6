import json
import time
import logging
import threading
from adc_reader import ADCReader
from hydraulic_controller import HydraulicController
from mqtt_client import MQTTClient
from config_loader import get_config

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(name)s] %(levelname)s: %(message)s"
)
logger = logging.getLogger("gateway")


class Gateway:
    def __init__(self):
        self.config = get_config()
        self.adc = ADCReader()
        self.hydraulic = HydraulicController()
        self.mqtt = MQTTClient(on_command_callback=self._handle_command)
        self._running = False
        self._report_interval = self.config["report"]["interval_sec"]
        self._hydraulic_states = {}

    def _handle_command(self, topic, payload):
        parts = topic.split("/")
        if len(parts) >= 3:
            underpass_id = parts[2]
        else:
            underpass_id = payload.get("underpassId", "unknown")

        action = payload.get("action", "").lower()
        logger.info("Command for %s: action=%s", underpass_id, action)

        if action == "lift":
            current = self._hydraulic_states.get(underpass_id, "lowered")
            if current == "lifted":
                logger.info("Underpass %s already lifted, skip", underpass_id)
                return
            threading.Thread(
                target=self._execute_lift, args=(underpass_id,), daemon=True
            ).start()
        elif action == "lower":
            threading.Thread(
                target=self._execute_lower, args=(underpass_id,), daemon=True
            ).start()
        else:
            logger.warning("Unknown action: %s", action)

    def _execute_lift(self, underpass_id):
        try:
            self.mqtt.publish_hydraulic_status(
                underpass_id, "lifting", 0, "in_progress"
            )
            result = self.hydraulic.lift()
            self._hydraulic_states[underpass_id] = "lifted"
            self.mqtt.publish_hydraulic_status(
                underpass_id, result["action"], result["height_cm"], result["status"]
            )
        except Exception as e:
            logger.error("Lift failed for %s: %s", underpass_id, e)
            self.mqtt.publish_hydraulic_status(
                underpass_id, "lift", 0, "failed"
            )

    def _execute_lower(self, underpass_id):
        try:
            self.mqtt.publish_hydraulic_status(
                underpass_id, "lowering", 0, "in_progress"
            )
            result = self.hydraulic.lower()
            self._hydraulic_states[underpass_id] = "lowered"
            self.mqtt.publish_hydraulic_status(
                underpass_id, result["action"], result["height_cm"], result["status"]
            )
        except Exception as e:
            logger.error("Lower failed for %s: %s", underpass_id, e)
            self.mqtt.publish_hydraulic_status(
                underpass_id, "lower", 0, "failed"
            )

    def _read_and_publish(self):
        sensors = self.config["sensors"]
        for sensor in sensors:
            try:
                depth_mm = self.adc.read_water_depth_mm(
                    sensor["adc_channel"], sensor.get("offset_mm", 0)
                )
                self.mqtt.publish_sensor_data(
                    sensor["underpass_id"], sensor["sensor_id"], depth_mm
                )
                logger.info(
                    "Sensor %s @ %s: %.1f mm",
                    sensor["sensor_id"], sensor["underpass_id"], depth_mm
                )
            except Exception as e:
                logger.error("Read sensor %s failed: %s", sensor["sensor_id"], e)

    def _report_loop(self):
        while self._running:
            try:
                self._read_and_publish()
            except Exception as e:
                logger.error("Report loop error: %s", e)
            time.sleep(self._report_interval)

    def start(self):
        logger.info("Starting IoT Gateway...")
        self.mqtt.connect()
        time.sleep(1)
        self._running = True
        self._report_thread = threading.Thread(
            target=self._report_loop, daemon=True
        )
        self._report_thread.start()
        logger.info("Gateway started, reporting every %ds", self._report_interval)

        try:
            while self._running:
                time.sleep(1)
        except KeyboardInterrupt:
            logger.info("Shutting down...")
            self.stop()

    def stop(self):
        self._running = False
        self.mqtt.disconnect()
        self.adc.close()
        self.hydraulic.cleanup()
        logger.info("Gateway stopped")


if __name__ == "__main__":
    gateway = Gateway()
    gateway.start()

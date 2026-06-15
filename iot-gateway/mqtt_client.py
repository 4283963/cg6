import json
import time
import threading
import logging
import paho.mqtt.client as mqtt
from config_loader import get_config

logger = logging.getLogger("mqtt_client")


class MQTTClient:
    def __init__(self, on_command_callback=None):
        cfg = get_config()["mqtt"]
        self.broker = cfg["broker"]
        self.port = cfg["port"]
        self.username = cfg.get("username", "")
        self.password = cfg.get("password", "")
        self.client_id = cfg["client_id_prefix"] + str(int(time.time()))
        self.keepalive = cfg["keepalive"]
        self.on_command_callback = on_command_callback

        topics = get_config()["topics"]
        self.data_topic = topics["data_publish"]
        self.command_topic = topics["command_subscribe"]
        self.status_topic = topics["status_publish"]

        self.client = mqtt.Client(client_id=self.client_id)
        if self.username:
            self.client.username_pw_set(self.username, self.password)
        self.client.on_connect = self._on_connect
        self.client.on_message = self._on_message
        self.client.on_disconnect = self._on_disconnect
        self._connected = False

    def _on_connect(self, client, userdata, flags, rc):
        if rc == 0:
            logger.info("Connected to MQTT broker %s:%d", self.broker, self.port)
            self._connected = True
            self.client.subscribe(self.command_topic, qos=1)
            logger.info("Subscribed to command topic: %s", self.command_topic)
        else:
            logger.error("MQTT connection failed, rc=%d", rc)

    def _on_message(self, client, userdata, msg):
        try:
            payload = json.loads(msg.payload.decode("utf-8"))
            logger.info("Received command on %s: %s", msg.topic, payload)
            if self.on_command_callback:
                self.on_command_callback(msg.topic, payload)
        except Exception as e:
            logger.error("Error processing command: %s", e)

    def _on_disconnect(self, client, userdata, rc):
        self._connected = False
        logger.warning("Disconnected from MQTT broker, rc=%d", rc)

    def connect(self):
        self.client.connect(self.broker, self.port, self.keepalive)
        self.client.loop_start()

    def disconnect(self):
        self.client.loop_stop()
        self.client.disconnect()

    def publish_sensor_data(self, underpass_id, sensor_id, depth_mm):
        payload = {
            "underpassId": underpass_id,
            "sensorId": sensor_id,
            "depthMm": depth_mm,
            "timestamp": int(time.time() * 1000)
        }
        self.client.publish(self.data_topic, json.dumps(payload), qos=1)
        logger.debug("Published sensor data: %s", payload)

    def publish_hydraulic_status(self, underpass_id, action, height_cm, status):
        payload = {
            "underpassId": underpass_id,
            "action": action,
            "heightCm": height_cm,
            "status": status,
            "timestamp": int(time.time() * 1000)
        }
        self.client.publish(self.status_topic, json.dumps(payload), qos=1)
        logger.info("Published hydraulic status: %s", payload)

    def is_connected(self):
        return self._connected

import time
import threading
from config_loader import get_config

try:
    import RPi.GPIO as GPIO
    GPIO.setmode(GPIO.BCM)
    GPIO.setwarnings(False)
    _ON_RPI = True
except (ImportError, RuntimeError):
    _ON_RPI = False


class HydraulicController:
    LIFT = "lift"
    LOWER = "lower"
    STOP = "stop"

    def __init__(self):
        cfg = get_config()["hydraulic"]
        self.relay_up_gpio = cfg["gpio_relay_up"]
        self.relay_down_gpio = cfg["gpio_relay_down"]
        self.lift_height_cm = cfg["lift_height_cm"]
        self.lift_duration = cfg["lift_duration_sec"]
        self.lower_duration = cfg["lower_duration_sec"]
        self._current_state = self.STOP
        self._lock = threading.Lock()
        self._init_gpio()

    def _init_gpio(self):
        if _ON_RPI:
            GPIO.setup(self.relay_up_gpio, GPIO.OUT, initial=GPIO.HIGH)
            GPIO.setup(self.relay_down_gpio, GPIO.OUT, initial=GPIO.HIGH)

    def _set_relay(self, up_active, down_active):
        if _ON_RPI:
            GPIO.output(self.relay_up_gpio, GPIO.LOW if up_active else GPIO.HIGH)
            GPIO.output(self.relay_down_gpio, GPIO.LOW if down_active else GPIO.HIGH)
        else:
            state = "UP" if up_active else ("DOWN" if down_active else "STOP")
            print(f"[Hydraulic] relay -> {state} (simulated)")

    def lift(self):
        with self._lock:
            self._set_relay(True, False)
            self._current_state = self.LIFT
            time.sleep(self.lift_duration)
            self._set_relay(False, False)
            self._current_state = self.STOP
        return {"action": self.LIFT, "height_cm": self.lift_height_cm, "status": "completed"}

    def lower(self):
        with self._lock:
            self._set_relay(False, True)
            self._current_state = self.LOWER
            time.sleep(self.lower_duration)
            self._set_relay(False, False)
            self._current_state = self.STOP
        return {"action": self.LOWER, "height_cm": 0, "status": "completed"}

    def get_state(self):
        return self._current_state

    def cleanup(self):
        self._set_relay(False, False)
        if _ON_RPI:
            GPIO.cleanup([self.relay_up_gpio, self.relay_down_gpio])

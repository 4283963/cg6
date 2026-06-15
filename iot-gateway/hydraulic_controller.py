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
    PRELIFT = "prelift"
    LOWER = "lower"
    STOP = "stop"

    def __init__(self):
        cfg = get_config()["hydraulic"]
        self.relay_up_gpio = cfg["gpio_relay_up"]
        self.relay_down_gpio = cfg["gpio_relay_down"]
        self.default_lift_height_cm = cfg["lift_height_cm"]
        self.cm_per_sec = cfg["lift_height_cm"] / cfg["lift_duration_sec"]
        self.lower_duration = cfg["lower_duration_sec"]
        self._current_state = self.STOP
        self._current_height_cm = 0
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

    def lift(self, target_height_cm=None):
        with self._lock:
            if target_height_cm is None:
                target_height_cm = self.default_lift_height_cm

            if self._current_height_cm >= target_height_cm:
                return {
                    "action": self.LIFT,
                    "height_cm": self._current_height_cm,
                    "target_cm": target_height_cm,
                    "status": "skipped_already_at_height"
                }

            delta_cm = target_height_cm - self._current_height_cm
            duration = max(0.5, delta_cm / self.cm_per_sec)

            action = self.PRELIFT if target_height_cm < self.default_lift_height_cm else self.LIFT

            print(f"[Hydraulic] lifting {delta_cm:.1f}cm to {target_height_cm}cm, will take {duration:.1f}s")

            self._set_relay(True, False)
            self._current_state = action
            time.sleep(duration)
            self._set_relay(False, False)
            self._current_state = self.STOP
            self._current_height_cm = target_height_cm

        return {
            "action": action,
            "height_cm": target_height_cm,
            "target_cm": target_height_cm,
            "status": "completed"
        }

    def lower(self):
        with self._lock:
            if self._current_height_cm <= 0:
                return {
                    "action": self.LOWER,
                    "height_cm": 0,
                    "status": "skipped_already_lowered"
                }

            self._set_relay(False, True)
            self._current_state = self.LOWER
            time.sleep(self.lower_duration)
            self._set_relay(False, False)
            self._current_state = self.STOP
            self._current_height_cm = 0

        return {"action": self.LOWER, "height_cm": 0, "status": "completed"}

    def get_state(self):
        return self._current_state

    def get_current_height_cm(self):
        return self._current_height_cm

    def cleanup(self):
        self._set_relay(False, False)
        if _ON_RPI:
            GPIO.cleanup([self.relay_up_gpio, self.relay_down_gpio])

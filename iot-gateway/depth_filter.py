import logging
from collections import deque

logger = logging.getLogger("depth_filter")


class DepthFilter:
    def __init__(self, window_size=5, spike_ratio=2.0):
        self.window_size = window_size
        self.spike_ratio = spike_ratio
        self._windows = {}
        self._last_stable = {}

    def filter(self, sensor_id, raw_mm):
        if sensor_id not in self._windows:
            self._windows[sensor_id] = deque(maxlen=self.window_size)
            self._last_stable[sensor_id] = raw_mm

        window = self._windows[sensor_id]
        last_stable = self._last_stable[sensor_id]

        validated = raw_mm
        if last_stable > 0:
            ratio = raw_mm / last_stable
            if ratio > self.spike_ratio or ratio < (1.0 / self.spike_ratio):
                logger.warning(
                    "Spike on %s: raw=%.1fmm, stable=%.1fmm, ratio=%.2f — clamped",
                    sensor_id, raw_mm, last_stable, ratio
                )
                validated = last_stable

        window.append(validated)

        smoothed = sum(window) / len(window)
        self._last_stable[sensor_id] = smoothed

        if abs(raw_mm - smoothed) > 20:
            logger.info(
                "Filtered %s: raw=%.1f -> smoothed=%.1f",
                sensor_id, raw_mm, smoothed
            )

        return round(smoothed, 1)

import spidev
import time
from config_loader import get_config


class ADCReader:
    def __init__(self):
        cfg = get_config()["adc"]
        self.spi = spidev.SpiDev()
        self.spi.open(cfg["spi_bus"], cfg["spi_device"])
        self.spi.max_speed_hz = 1350000
        self.vref = cfg["vref"]

    def _read_raw(self, channel):
        adc_channel = max(0, min(7, channel))
        cmd = [1, (8 + adc_channel) << 4, 0]
        result = self.spi.xfer2(cmd)
        raw = ((result[1] & 3) << 8) + result[2]
        return raw

    def read_voltage(self, channel):
        raw = self._read_raw(channel)
        voltage = (raw / 1023.0) * self.vref
        return round(voltage, 4)

    def read_water_depth_mm(self, channel, offset_mm=0):
        voltage = self.read_voltage(channel)
        depth_mm = (voltage / self.vref) * 5000.0 + offset_mm
        return round(max(0, depth_mm), 1)

    def close(self):
        self.spi.close()

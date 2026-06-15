import yaml
import os

_config = None

CONFIG_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "config.yaml")


def load_config(path=None):
    global _config
    p = path or CONFIG_PATH
    with open(p, "r", encoding="utf-8") as f:
        _config = yaml.safe_load(f)
    return _config


def get_config():
    global _config
    if _config is None:
        _config = load_config()
    return _config

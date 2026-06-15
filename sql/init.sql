CREATE DATABASE IF NOT EXISTS underpass_flood
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE underpass_flood;

CREATE TABLE IF NOT EXISTS underpass_info (
  id                    VARCHAR(32)  NOT NULL PRIMARY KEY COMMENT '地道桥编号',
  name                  VARCHAR(64)  NOT NULL COMMENT '地道桥名称',
  longitude             DOUBLE       NOT NULL COMMENT '经度',
  latitude              DOUBLE       NOT NULL COMMENT '纬度',
  manhole_count         INT          NOT NULL DEFAULT 3 COMMENT '液压井盖数量',
  led_id                VARCHAR(32)  DEFAULT NULL COMMENT 'LED警示灯编号',
  upstream_catchment_id VARCHAR(32)  DEFAULT NULL COMMENT '关联上游汇水区编号',
  status                VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL / ALARM',
  hydraulic_state       VARCHAR(16)  NOT NULL DEFAULT 'LOWERED' COMMENT 'LOWERED / PRE_LIFTED / FULL_LIFTED',
  last_alarm_time       DATETIME     DEFAULT NULL COMMENT '最近告警时间',
  last_forecast_time    DATETIME     DEFAULT NULL COMMENT '最近前瞻预警时间',
  create_time           DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_time           DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='地道桥基本信息';

CREATE TABLE IF NOT EXISTS upstream_catchment (
  id              VARCHAR(32)  NOT NULL PRIMARY KEY COMMENT '上游汇水区编号',
  name            VARCHAR(64)  NOT NULL COMMENT '汇水区/排水渠名称',
  flow_meter_id   VARCHAR(64)  NOT NULL COMMENT '流量计编号',
  rain_sensor_id  VARCHAR(32)  DEFAULT NULL COMMENT '雨量传感器编号',
  longitude       DOUBLE       NOT NULL COMMENT '经度',
  latitude        DOUBLE       NOT NULL COMMENT '纬度',
  description     VARCHAR(128) DEFAULT NULL COMMENT '描述',
  create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='上游汇水区信息';

CREATE TABLE IF NOT EXISTS water_depth_record (
  id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  underpass_id   VARCHAR(32)  NOT NULL COMMENT '地道桥编号',
  sensor_id      VARCHAR(32)  NOT NULL COMMENT '传感器编号',
  depth_mm       DOUBLE       NOT NULL COMMENT '积水深度(mm)',
  timestamp_ms   BIGINT       DEFAULT NULL COMMENT '传感器采集时间戳',
  received_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '后端接收时间',
  INDEX idx_underpass_time (underpass_id, received_at DESC)
) ENGINE=InnoDB COMMENT='积水深度记录';

CREATE TABLE IF NOT EXISTS flow_record (
  id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  catchment_id   VARCHAR(32)  NOT NULL COMMENT '汇水区编号',
  flow_meter_id  VARCHAR(64)  NOT NULL COMMENT '流量计编号',
  flow_rate_lps  DOUBLE       NOT NULL COMMENT '流量(L/s)',
  timestamp_ms   BIGINT       DEFAULT NULL COMMENT '采集时间戳',
  received_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '后端接收时间',
  INDEX idx_catchment_time (catchment_id, received_at DESC)
) ENGINE=InnoDB COMMENT='上游排水渠流量记录';

CREATE TABLE IF NOT EXISTS rainfall_record (
  id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  underpass_id     VARCHAR(32)  NOT NULL COMMENT '地道桥编号',
  sensor_id        VARCHAR(32)  DEFAULT NULL COMMENT '雨量传感器编号',
  raining          TINYINT(1)   NOT NULL COMMENT '是否下雨',
  rain_mm_per_hour DOUBLE       NOT NULL DEFAULT 0 COMMENT '小时降雨量(mm)',
  timestamp_ms     BIGINT       DEFAULT NULL COMMENT '采集时间戳',
  received_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '后端接收时间',
  INDEX idx_underpass_time (underpass_id, received_at DESC)
) ENGINE=InnoDB COMMENT='降雨记录';

CREATE TABLE IF NOT EXISTS hydraulic_action_log (
  id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  underpass_id   VARCHAR(32)  NOT NULL COMMENT '地道桥编号',
  action         VARCHAR(16)  NOT NULL COMMENT 'prelift / lift / lower',
  height_cm      INT          NOT NULL DEFAULT 0 COMMENT '顶升高度(cm)',
  status         VARCHAR(16)  DEFAULT NULL COMMENT 'dispatched / completed / failed',
  trigger_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间',
  complete_time  DATETIME     DEFAULT NULL COMMENT '完成时间',
  INDEX idx_underpass (underpass_id)
) ENGINE=InnoDB COMMENT='液压顶升操作日志';

CREATE TABLE IF NOT EXISTS led_control_log (
  id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  underpass_id   VARCHAR(32)  NOT NULL COMMENT '地道桥编号',
  led_id         VARCHAR(32)  NOT NULL COMMENT 'LED灯编号',
  mode           VARCHAR(16)  NOT NULL COMMENT 'ALARM / FORECAST / NORMAL',
  display_text   VARCHAR(128) DEFAULT NULL COMMENT '显示文字',
  color          VARCHAR(16)  DEFAULT NULL COMMENT 'RED / YELLOW / GREEN',
  result         VARCHAR(16)  DEFAULT NULL COMMENT 'SUCCESS / FAILED / API_ERROR',
  operate_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_underpass (underpass_id)
) ENGINE=InnoDB COMMENT='LED警示灯控制日志';

INSERT INTO underpass_info (id, name, longitude, latitude, manhole_count, led_id, status, hydraulic_state) VALUES
('UP-BRIDGE-01', '建设路地道桥', 116.4074, 39.9042, 3, 'LED-001', 'NORMAL', 'LOWERED'),
('UP-BRIDGE-02', '人民路地道桥', 116.4127, 39.9087, 4, 'LED-002', 'NORMAL', 'LOWERED'),
('UP-BRIDGE-03', '解放路地道桥', 116.3978, 39.9156, 2, 'LED-003', 'NORMAL', 'LOWERED'),
('UP-BRIDGE-04', '中山路地道桥', 116.4215, 39.8976, 3, 'LED-004', 'NORMAL', 'LOWERED'),
('UP-BRIDGE-05', '光明路地道桥', 116.3892, 39.9120, 2, 'LED-005', 'NORMAL', 'LOWERED');

INSERT INTO upstream_catchment (id, name, flow_meter_id, rain_sensor_id, longitude, latitude, description) VALUES
('CATCH-001', '建设路上游排水渠', 'FM-001', 'RAIN-001', 116.4100, 39.9100, '建设路地道桥上游露天排水渠'),
('CATCH-002', '人民路上游排水渠', 'FM-002', 'RAIN-002', 116.4180, 39.9150, '人民路地道桥上游露天排水渠'),
('CATCH-003', '解放路上游排水渠', 'FM-003', 'RAIN-003', 116.3950, 39.9200, '解放路地道桥上游露天排水渠'),
('CATCH-004', '中山路上游排水渠', 'FM-004', 'RAIN-004', 116.4250, 39.9050, '中山路地道桥上游露天排水渠'),
('CATCH-005', '光明路上游排水渠', 'FM-005', 'RAIN-005', 116.3850, 39.9180, '光明路地道桥上游露天排水渠');

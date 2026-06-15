CREATE DATABASE IF NOT EXISTS underpass_flood
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE underpass_flood;

CREATE TABLE IF NOT EXISTS underpass_info (
  id              VARCHAR(32)  NOT NULL PRIMARY KEY COMMENT '地道桥编号',
  name            VARCHAR(64)  NOT NULL COMMENT '地道桥名称',
  longitude       DOUBLE       NOT NULL COMMENT '经度',
  latitude        DOUBLE       NOT NULL COMMENT '纬度',
  manhole_count   INT          NOT NULL DEFAULT 3 COMMENT '液压井盖数量',
  led_id          VARCHAR(32)  DEFAULT NULL COMMENT 'LED警示灯编号',
  status          VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL / ALARM',
  last_alarm_time DATETIME     DEFAULT NULL COMMENT '最近告警时间',
  create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='地道桥基本信息';

CREATE TABLE IF NOT EXISTS water_depth_record (
  id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  underpass_id   VARCHAR(32)  NOT NULL COMMENT '地道桥编号',
  sensor_id      VARCHAR(32)  NOT NULL COMMENT '传感器编号',
  depth_mm       DOUBLE       NOT NULL COMMENT '积水深度(mm)',
  timestamp_ms   BIGINT       DEFAULT NULL COMMENT '传感器采集时间戳',
  received_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '后端接收时间',
  INDEX idx_underpass_time (underpass_id, received_at DESC)
) ENGINE=InnoDB COMMENT='积水深度记录';

CREATE TABLE IF NOT EXISTS hydraulic_action_log (
  id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  underpass_id   VARCHAR(32)  NOT NULL COMMENT '地道桥编号',
  action         VARCHAR(16)  NOT NULL COMMENT 'lift / lower',
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
  mode           VARCHAR(16)  NOT NULL COMMENT 'ALARM / NORMAL',
  display_text   VARCHAR(128) DEFAULT NULL COMMENT '显示文字',
  color          VARCHAR(16)  DEFAULT NULL COMMENT 'RED / GREEN',
  result         VARCHAR(16)  DEFAULT NULL COMMENT 'SUCCESS / FAILED / API_ERROR',
  operate_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_underpass (underpass_id)
) ENGINE=InnoDB COMMENT='LED警示灯控制日志';

INSERT INTO underpass_info (id, name, longitude, latitude, manhole_count, led_id, status) VALUES
('UP-BRIDGE-01', '建设路地道桥', 116.4074, 39.9042, 3, 'LED-001', 'NORMAL'),
('UP-BRIDGE-02', '人民路地道桥', 116.4127, 39.9087, 4, 'LED-002', 'NORMAL'),
('UP-BRIDGE-03', '解放路地道桥', 116.3978, 39.9156, 2, 'LED-003', 'NORMAL'),
('UP-BRIDGE-04', '中山路地道桥', 116.4215, 39.8976, 3, 'LED-004', 'NORMAL'),
('UP-BRIDGE-05', '光明路地道桥', 116.3892, 39.9120, 2, 'LED-005', 'NORMAL');

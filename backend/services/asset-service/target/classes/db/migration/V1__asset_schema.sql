CREATE TABLE assets (
  id BINARY(16) NOT NULL PRIMARY KEY,
  property_id BINARY(16) NOT NULL,
  unit_id BINARY(16) NULL,
  name VARCHAR(160) NOT NULL,
  category VARCHAR(80) NOT NULL,
  serial_number VARCHAR(120) NULL,
  location VARCHAR(200) NOT NULL,
  status VARCHAR(32) NOT NULL,
  installed_on DATE NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  INDEX idx_assets_property_status (property_id, status),
  INDEX idx_assets_property_category (property_id, category)
);
CREATE TABLE maintenance_schedules (
  id BINARY(16) NOT NULL PRIMARY KEY,
  asset_id BINARY(16) NOT NULL,
  title VARCHAR(160) NOT NULL,
  description VARCHAR(1000) NOT NULL,
  interval_days INT NOT NULL,
  start_date DATE NOT NULL,
  next_due_date DATE NOT NULL,
  last_completed_date DATE NULL,
  active BOOLEAN NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  INDEX idx_schedules_asset_active (asset_id, active)
);
CREATE TABLE maintenance_tasks (
  id BINARY(16) NOT NULL PRIMARY KEY,
  schedule_id BINARY(16) NOT NULL,
  asset_id BINARY(16) NOT NULL,
  property_id BINARY(16) NOT NULL,
  due_date DATE NOT NULL,
  status VARCHAR(24) NOT NULL,
  completed_at DATETIME(6) NULL,
  INDEX idx_tasks_property_status_due (property_id, status, due_date),
  INDEX idx_tasks_schedule_status (schedule_id, status)
);
CREATE TABLE service_records (
  id BINARY(16) NOT NULL PRIMARY KEY,
  asset_id BINARY(16) NOT NULL,
  task_id BINARY(16) NULL,
  performed_by BINARY(16) NOT NULL,
  performed_on DATE NOT NULL,
  summary VARCHAR(1000) NOT NULL,
  cost DECIMAL(12,2) NULL,
  created_at DATETIME(6) NOT NULL,
  INDEX idx_service_records_asset_date (asset_id, performed_on),
  INDEX idx_service_records_task (task_id)
);

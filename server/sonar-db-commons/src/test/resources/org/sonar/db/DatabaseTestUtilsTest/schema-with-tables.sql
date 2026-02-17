-- Test schema for DatabaseTestUtilsTest

CREATE TABLE test_table (
  id INTEGER PRIMARY KEY,
  name VARCHAR(255)
);

CREATE TABLE another_table (
  id INTEGER PRIMARY KEY,
  value VARCHAR(255)
);

-- These tables should be filtered out by loadTableNames()
CREATE TABLE schema_migrations (
  version VARCHAR(255) PRIMARY KEY
);

CREATE TABLE flyway_schema_history (
  installed_rank INTEGER PRIMARY KEY,
  version VARCHAR(50),
  description VARCHAR(200),
  type VARCHAR(20),
  script VARCHAR(1000),
  checksum INTEGER,
  installed_by VARCHAR(100),
  installed_on TIMESTAMP,
  execution_time INTEGER,
  success BOOLEAN
);

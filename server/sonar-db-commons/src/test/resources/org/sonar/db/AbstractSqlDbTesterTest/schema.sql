CREATE TABLE test_table (
  id INTEGER NOT NULL PRIMARY KEY,
  col1 VARCHAR(100),
  col2 INTEGER
);

CREATE INDEX idx_col1 ON test_table(col1);

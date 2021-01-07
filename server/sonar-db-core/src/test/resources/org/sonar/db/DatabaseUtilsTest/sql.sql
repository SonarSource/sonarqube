CREATE TABLE "schema_migrations" (
  "version" VARCHAR(256) NOT NULL
);

CREATE TABLE "TABLEA" (
  "COLUMNA" VARCHAR(256) NOT NULL
);

CREATE INDEX UPPER_CASE_NAME ON schema_migrations (version);

CREATE INDEX lower_case_name ON schema_migrations (version);

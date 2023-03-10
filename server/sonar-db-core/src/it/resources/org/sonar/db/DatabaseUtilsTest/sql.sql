CREATE TABLE "schema_migrations" (
  "version" VARCHAR(256) NOT NULL
);

CREATE TABLE "TABLEA" (
  "COLUMNA" VARCHAR(256) NOT NULL,
  "COLUMNB" VARCHAR(256) NOT NULL
);

CREATE INDEX UPPER_CASE_NAME ON schema_migrations (version);

CREATE INDEX lower_case_name ON schema_migrations (version);

--For test on special index name
CREATE INDEX idx_1234_index_name ON schema_migrations (version);
CREATE INDEX idx_index_name_2 ON schema_migrations (version);
CREATE INDEX idx__index_name_2 ON schema_migrations (version);


CREATE INDEX test ON TABLEA(COLUMNA, COLUMNB);

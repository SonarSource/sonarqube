CREATE TABLE "QUALITY_GATE_CONDITIONS"(
                                          "UUID" CHARACTER VARYING(40) NOT NULL,
                                          "OPERATOR" CHARACTER VARYING(3),
                                          "VALUE_ERROR" CHARACTER VARYING(64),
                                          "CREATED_AT" TIMESTAMP,
                                          "UPDATED_AT" TIMESTAMP,
                                          "METRIC_UUID" CHARACTER VARYING(40) NOT NULL,
                                          "QGATE_UUID" CHARACTER VARYING(40) NOT NULL
);
ALTER TABLE "QUALITY_GATE_CONDITIONS" ADD CONSTRAINT "PK_QUALITY_GATE_CONDITIONS" PRIMARY KEY("UUID");

CREATE TABLE "QUALITY_GATES"(
                                "UUID" CHARACTER VARYING(40) NOT NULL,
                                "NAME" CHARACTER VARYING(100) NOT NULL,
                                "IS_BUILT_IN" BOOLEAN NOT NULL,
                                "CREATED_AT" TIMESTAMP,
                                "UPDATED_AT" TIMESTAMP,
                                "AI_CODE_SUPPORTED" BOOLEAN DEFAULT FALSE NOT NULL
);
ALTER TABLE "QUALITY_GATES" ADD CONSTRAINT "PK_QUALITY_GATES" PRIMARY KEY("UUID");

CREATE TABLE "METRICS"(
                          "UUID" CHARACTER VARYING(40) NOT NULL,
                          "NAME" CHARACTER VARYING(64) NOT NULL,
                          "DESCRIPTION" CHARACTER VARYING(255),
                          "DIRECTION" INTEGER DEFAULT 0 NOT NULL,
                          "DOMAIN" CHARACTER VARYING(64),
                          "SHORT_NAME" CHARACTER VARYING(64),
                          "QUALITATIVE" BOOLEAN DEFAULT FALSE NOT NULL,
                          "VAL_TYPE" CHARACTER VARYING(8),
                          "ENABLED" BOOLEAN DEFAULT TRUE,
                          "WORST_VALUE" DOUBLE PRECISION,
                          "BEST_VALUE" DOUBLE PRECISION,
                          "OPTIMIZED_BEST_VALUE" BOOLEAN,
                          "HIDDEN" BOOLEAN,
                          "DELETE_HISTORICAL_DATA" BOOLEAN,
                          "DECIMAL_SCALE" INTEGER
);
ALTER TABLE "METRICS" ADD CONSTRAINT "PK_METRICS" PRIMARY KEY("UUID");
CREATE UNIQUE NULLS NOT DISTINCT INDEX "METRICS_UNIQUE_NAME" ON "METRICS"("NAME" NULLS FIRST);

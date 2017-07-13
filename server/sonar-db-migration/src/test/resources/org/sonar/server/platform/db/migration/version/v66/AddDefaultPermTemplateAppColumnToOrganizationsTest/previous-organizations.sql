CREATE TABLE "ORGANIZATIONS" (
  "UUID" VARCHAR(40) NOT NULL PRIMARY KEY,
  "KEE" VARCHAR(32) NOT NULL,
  "NAME" VARCHAR(64) NOT NULL,
  "DESCRIPTION" VARCHAR(256),
  "URL" VARCHAR(256),
  "AVATAR_URL" VARCHAR(256),
  "GUARDED" BOOLEAN NOT NULL,
  "USER_ID" INTEGER,
  "DEFAULT_PERM_TEMPLATE_PROJECT" VARCHAR(40),
  "DEFAULT_PERM_TEMPLATE_VIEW" VARCHAR(40),
  "DEFAULT_GROUP_ID" INTEGER,
  "NEW_PROJECT_PRIVATE" BOOLEAN NOT NULL,
  "CREATED_AT" BIGINT NOT NULL,
  "UPDATED_AT" BIGINT NOT NULL
);

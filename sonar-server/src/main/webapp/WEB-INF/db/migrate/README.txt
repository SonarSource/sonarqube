Don't forget that index name limited to 30 characters in Oracle DB.

Prefer to add nullable columns to avoid problems during migration.

After adding migration script - don't forget to update sonar-core/src/main/java/org/sonar/jpa/entity/SchemaMigration.java and ../structure/derby.*

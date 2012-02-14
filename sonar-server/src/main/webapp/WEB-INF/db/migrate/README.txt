HOW TO ADD A MIGRATION

* Jump some versions when adding the first Ruby on Rails migration of a new sonar version. For example if sonar 2.10 is 193, then sonar 2.11 should start at 200.
* Complete the DDL files for Derby :
  + sonar-core/src/main/resources/org/sonar/core/persistence/schema-derby.ddl
  + sonar-core/src/main/resources/org/sonar/core/persistence/rows-derby.sql :
    - add "INSERT INTO SCHEMA_MIGRATIONS(VERSION) VALUES ('<THE MIGRATION ID>')"
* Update the migration id defined in sonar-core/src/main/java/org/sonar/jpa/entity/SchemaMigration.java
* If a table is added or removed, then edit sonar-core/src/main/java/org/sonar/core/persistence/DatabaseUtils.java



RECOMMANDATIONS

* Don't forget that index name limited to 30 characters in Oracle DB.
* Prefer to add nullable columns to avoid problems during migration.
* When adding index, do not forget to name the index (so that it is possible later to delete it)
  + Example: 
      add_index "action_plans", "project_id", :name => "ap_project_id"
* When modifying columns in a table, do not forget to reset the column information on the Ruby model if you use it 
  + Example:
      add_column 'users', 'active', :boolean, :null => true, :default => true
      User.reset_column_information

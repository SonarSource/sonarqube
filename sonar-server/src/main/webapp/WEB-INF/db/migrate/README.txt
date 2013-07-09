HOW TO ADD A MIGRATION

* Jump some versions when adding the first Ruby on Rails migration of a new sonar version. For example if sonar 2.10 is 193, then sonar 2.11 should start at 200.
* Complete the DDL files for H2 :
  + sonar-core/src/main/resources/org/sonar/core/persistence/schema-h2.ddl
  + sonar-core/src/main/resources/org/sonar/core/persistence/rows-h2.sql :
    - add "INSERT INTO SCHEMA_MIGRATIONS(VERSION) VALUES ('<THE MIGRATION ID>')"
* Update the migration id defined in sonar-core/src/main/java/org/sonar/core/persistence/DatabaseVersion.java
* If a table is added or removed, then edit sonar-core/src/main/java/org/sonar/core/persistence/DatabaseUtils.java


RECOMMENDATIONS

* Prefer to add nullable columns to avoid problems during migration, EXCEPT for booleans. For booleans:

  * columns must be NON-nullable but default value (false) must NOT be set in database. It allows to fully define the model programmatically.
  * column names must be chosen so that the default value is actually false.
    * E.g.: rule_failures.switched_off

* Always create an index with a name : add_index "action_plans", "project_id", :name => "action_plans_project_id"
  Note that this name is limited to 30 characters because of Oracle constraint.

* Silently ignore failures when adding an index that has already been created by users. It can occur when the index
  is not created in the same migration script than the table.

  begin
    add_index "action_plans", "project_id", :name => "action_plans_project_id"
  rescue
    # ignore
  end

* Use faux models when touching rows (SELECT/INSERT/UPDATE/DELETE). See http://guides.rubyonrails.org/migrations.html#using-models-in-your-migrations
  for more details. Note that associations must not be used.
  IMPORTANT : do not use faux models for user models (User, Group, UserRole, GroupRole) because of required associations and password encryption.


  class MyMigration < ActiveRecord::Migration
    # This is the faux model. It only maps columns. No functional methods.
    class Metric < ActiveRecord::Base
    end

    def self.up
      # it’s a good idea to call reset_column_information to refresh the ActiveRecord cache for the model prior to
      # updating data in the database
      Metric.reset_column_information
      Metric.find(:all) do |m|
        m.save
      end
    end
  end
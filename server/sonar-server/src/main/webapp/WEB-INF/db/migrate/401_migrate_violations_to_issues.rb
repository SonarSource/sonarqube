#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

#
# Sonar 3.6
# See SONAR-4305
#
class MigrateViolationsToIssues < ActiveRecord::Migration

  def self.up
    remove_index_quietly('rule_failure_snapshot_id')
    remove_index_quietly('rule_failure_rule_id')
    remove_index_quietly('rf_permanent_id')

    # Required for MSSQL to unlock the table RULE_FAILURES
    ActiveRecord::Base.connection.commit_db_transaction

    execute_java_migration('org.sonar.server.db.migrations.v36.ViolationMigration')

    # Currently not possible in Java because of Oracle (triggers and sequences must be dropped)
    drop_table('rule_failures')
    drop_table('reviews')
    drop_table('review_comments')
    drop_table('action_plans_reviews')

    add_index :issues,  :kee,                 :name => 'issues_kee',         :unique => true
    add_index :issues,  :component_id,        :name => 'issues_component_id'
    add_index :issues,  :root_component_id,   :name => 'issues_root_component_id'
    add_index :issues,  :rule_id,             :name => 'issues_rule_id'
    add_index :issues,  :severity,            :name => 'issues_severity'
    add_index :issues,  :status,              :name => 'issues_status'
    add_index :issues,  :resolution,          :name => 'issues_resolution'
    add_index :issues,  :assignee,            :name => 'issues_assignee'
    add_index :issues,  :action_plan_key,     :name => 'issues_action_plan_key'
    add_index :issues,  :issue_creation_date, :name => 'issues_creation_date'
  end


  def self.remove_index_quietly(name)
    begin
      remove_index('rule_failures', :name => name)
    rescue
      # probably already removed
    end
  end
end

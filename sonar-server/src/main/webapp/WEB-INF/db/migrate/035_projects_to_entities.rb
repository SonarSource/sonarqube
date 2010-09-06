#
# Sonar, entreprise quality control tool.
# Copyright (C) 2009 SonarSource SA
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#

require 'project'

class Project
  def branch
    read_attribute(:branch)
  end
end

class ProjectsToEntities < ActiveRecord::Migration
  def self.up
    add_column 'projects', 'scope', :string, :limit => 3
    add_column 'projects', 'qualifier', :string, :limit => 3
    add_column 'projects', 'kee', :string, :limit => 230
    add_column 'projects', 'root_id', :integer

    remove_column 'projects', 'group_id'
    remove_column 'projects', 'artifact_id'
    remove_column 'projects', 'branch'
    Project.reset_column_information

    upgrade_snapshots
    move_file_sources_to_snapshot_sources

    migrate_distribution_data

    begin
      remove_index :project_measures, :name => 'project_measure_history_query'
    rescue
    end
  end

  def self.down
    raise ActiveRecord::IrreversibleMigration
  end

  private

  def self.migrate_distribution_data
    remove_column :project_measures, :subkey
    add_column :project_measures, :text_value, :string, :limit => 96, :null => true
    ProjectMeasure.reset_column_information
  end

  def self.upgrade_snapshots
    add_column 'snapshots', 'scope', :string, :limit => 3
    add_column 'snapshots', 'qualifier', :string, :limit => 3
    add_column 'snapshots', 'root_snapshot_id', :integer

    remove_column 'snapshots', 'version'
    add_column 'snapshots', 'version', :string, :limit => 32

    Snapshot.reset_column_information

    begin
      remove_index :snapshots, :name => 'snapshot_created_at'
    rescue
      # the index does not exist (from 1.3)
    end
    add_index :snapshots, :parent_snapshot_id, :name => 'snapshots_parent'
    add_index :snapshots, :root_snapshot_id, :name => 'snapshots_root'
  end

  def self.move_file_sources_to_snapshot_sources
    create_table :snapshot_sources do |t|
      t.column :snapshot_id, :integer,   :null => false
      t.column :data,        :text
    end
    add_index :snapshot_sources, :snapshot_id, :name => 'snap_sources_snapshot_id'

    drop_table 'files'

    begin
      remove_index :rule_failures, :name => 'rule_failure_file_id'
    rescue
    end

    remove_column 'rule_failures', 'file_id'
    add_index :rule_failure_params, :snapshot_id, :name => 'rule_fails_params_snap'
    RuleFailure.reset_column_information
  end

  class RuleFailureParam035 < ActiveRecord::Base
    set_table_name "rule_failure_params"
  end
  class RuleFailure035 < ActiveRecord::Base
    set_table_name "rule_failures"
  end
end
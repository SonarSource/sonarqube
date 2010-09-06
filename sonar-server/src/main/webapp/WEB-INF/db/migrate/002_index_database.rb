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
class IndexDatabase < ActiveRecord::Migration
  
  def self.up
    
    add_index :files, :snapshot_id, :name => 'file_snapshot_id'
        
    add_index :project_measures, :snapshot_id, :name => 'project_measure_snapshot_id'
    
    add_index :rule_failures, :snapshot_id, :name => 'rule_failure_snapshot_id'
    add_index :rule_failures, :rule_id, :name => 'rule_failure_rule_id'
    add_index :rules_parameters, :rule_id, :name => 'rules_parameters_rule_id'

    add_index :snapshots, :project_id, :name => 'snapshot_project_id'
    
    add_index :metrics, :name, :unique => true, :name => 'metrics_unique_name'       
  end

  def self.down
    remove_index :files, :name => 'file_snapshot_id'
    remove_index :project_measures, :name => 'project_measure_snapshot_id'
    remove_index :rule_failures, :name => 'rule_failure_snapshot_id'
    remove_index :rule_failures, :name => 'rule_failure_rule_id'
    remove_index :rules_parameters, :name => 'rules_parameters_rule_id'
    remove_index :snapshots, :name => 'snapshot_project_id'
    remove_index :metrics, :name => 'metrics_unique_name'  
  end
end
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
class IndexDatabase < ActiveRecord::Migration
  
  def self.up
    add_index 'projects', 'root_id', :name => 'projects_root_id'
    add_index :project_measures, [:snapshot_id, :metric_id], :name => 'measures_sid_metric'
    add_index :rules_parameters, :rule_id, :name => 'rules_parameters_rule_id'

    add_index :snapshots, :project_id, :name => 'snapshot_project_id'
    add_index :snapshots, :parent_snapshot_id, :name => 'snapshots_parent'
    add_index :snapshots, :root_snapshot_id, :name => 'snapshots_root'
    add_index :snapshots, :qualifier, :name => 'snapshots_qualifier'
    
    add_index :metrics, :name, :unique => true, :name => 'metrics_unique_name'       
  end
end

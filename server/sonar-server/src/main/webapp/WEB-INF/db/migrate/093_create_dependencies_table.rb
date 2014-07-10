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
# sonar 2.0
class CreateDependenciesTable < ActiveRecord::Migration

  def self.up
    create_table :dependencies do |t|
      t.column :from_snapshot_id, :integer, :null => true
      t.column :from_resource_id, :integer, :null => true
      t.column :to_snapshot_id, :integer, :null => true
      t.column :to_resource_id, :integer, :null => true
      t.column :dep_usage, :string, :null => true, :limit => 30
      t.column :dep_weight, :integer, :null => true
      t.column :project_snapshot_id, :integer, :null => true
      t.column :parent_dependency_id, :big_integer, :null => true
      t.column :from_scope, :string, :limit => 3, :null => true
      t.column :to_scope, :string, :limit => 3, :null => true
    end

    alter_to_big_primary_key('dependencies')
    add_index :dependencies, :from_snapshot_id, :name => 'deps_from_sid'
    add_index :dependencies, :to_snapshot_id, :name => 'deps_to_sid'
  end

end

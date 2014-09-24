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
# Sonar 2.13
#
class CreateTableResourceIndex < ActiveRecord::Migration

  def self.up
    create_table 'resource_index' do |t|
      t.column 'kee', :string, :null => false, :limit => 400
      t.column 'position', :integer, :null => false
      t.column 'name_size', :integer, :null => false
      t.column 'resource_id', :integer, :null => false
      t.column 'root_project_id', :integer, :null => false
      t.column 'qualifier', :string, :limit => 10, :null => false
    end

    add_varchar_index 'resource_index', 'kee', :name => 'resource_index_key'
    add_index 'resource_index', 'resource_id', :name => 'resource_index_rid'
  end

end

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
# Sonar 3.0
#
class AddResourceIndexPrimaryKey < ActiveRecord::Migration

  class ResourceIndex < ActiveRecord::Base
    set_table_name 'resource_index'
  end

  def self.up
    ResourceIndex.reset_column_information
    # upgrade from version < 2.13 -> the table did not exist and was created in script 237 with the primary key -> ignore
    # upgrade from version 2.13 or 2.14 -> the table existed without primary key -> drop and create again
    unless ResourceIndex.columns_hash.has_key?('id')

      # Release 2.13 creates the table without primary key.
      # Unfortunately it's tricky to add a primary key to an existing table,
      # particularly on Oracle (note that it's perfectly supported on postgresql).
      # For this reason the table is dropped and created again.
      remove_indices
      drop_table 'resource_index'

      create_table 'resource_index' do |t|
        t.column 'kee', :string, :null => false, :limit => 400
        t.column 'position', :integer, :null => false
        t.column 'name_size', :integer, :null => false
        t.column 'resource_id', :integer, :null => false
        t.column 'root_project_id', :integer, :null => false
        t.column 'qualifier', :string, :limit => 10, :null => false
      end

      # for unknown reason, indices can't be created here for Oracle (and as usual for Oracle only).
      # These indices are moved to script 286.
    end
  end

  private
  def self.remove_indices
    begin
      remove_index 'resource_index', :name => 'resource_index_key'
    rescue
      #ignore
    end
    begin
      remove_index 'resource_index', :name => 'resource_index_rid'
    rescue
      #ignore
    end
  end
end

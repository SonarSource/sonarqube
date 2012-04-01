#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
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

#
# Sonar 2.15
#
class AddResourceIndexPrimaryKey < ActiveRecord::Migration

  class ResourceIndex < ActiveRecord::Base
    set_table_name 'resource_index'
  end

  def self.up
    ResourceIndex.reset_column_information
    unless ResourceIndex.columns_hash.has_key?('id')

      # Release 2.13 creates the table without primary key.
      # Unfortunately it's tricky to add a primary key to an existing table,
      # particularly on Oracle (note that it's perfectly supported on postgresql).
      # For this reason the table is dropped and created again.
      remove_index 'resource_index', :name => 'resource_index_key'
      remove_index 'resource_index', :name => 'resource_index_rid'
      drop_table 'resource_index'

      create_table 'resource_index' do |t|
        t.column 'kee', :string, :null => false, :limit => 400
        t.column 'position', :integer, :null => false
        t.column 'name_size', :integer, :null => false
        t.column 'resource_id', :integer, :null => false
        t.column 'root_project_id', :integer, :null => false
        t.column 'qualifier', :string, :limit => 10, :null => false
      end

      say_with_time 'Indexing projects' do
        Java::OrgSonarServerUi::JRubyFacade.getInstance().indexProjects()
      end
    end
  end

end

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
# SonarQube 5.2
# SONAR-6255
#
class AddFileSourcesDataType < ActiveRecord::Migration

  def self.up
    add_column 'file_sources', 'data_type', :string, :limit => 20
    change_column 'file_sources', 'data_hash', :string, :limit => 50, :null => true
    remove_index_quietly('file_sources_file_uuid_uniq')
    add_index 'file_sources', ['file_uuid', 'data_type'], :name => 'file_sources_uuid_type', :unique => true
  end

  def self.remove_index_quietly(name)
    begin
      remove_index('file_sources', :name => name)
    rescue
      # probably already removed
    end
  end
end

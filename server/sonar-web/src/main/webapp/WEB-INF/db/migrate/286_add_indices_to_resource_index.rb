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
class AddIndicesToResourceIndex < ActiveRecord::Migration

  class ResourceIndex < ActiveRecord::Base
    set_table_name 'resource_index'
  end

  def self.up
    ResourceIndex.reset_column_information

    begin
      add_varchar_index 'resource_index', 'kee', :name => 'resource_index_key'
    rescue
      #ignore, already exists
    end
    begin
      add_index 'resource_index', 'resource_id', :name => 'resource_index_rid'
    rescue
      #ignore, already exists
    end
  end
end

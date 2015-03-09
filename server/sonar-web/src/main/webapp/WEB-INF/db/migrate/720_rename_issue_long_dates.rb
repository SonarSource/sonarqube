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
# SonarQube 5.0
#
class RenameIssueLongDates < ActiveRecord::Migration

  def self.up
    remove_index 'issues', :name => 'issues_updated_at'
    remove_column 'issues', 'created_at'
    remove_column 'issues', 'updated_at'
    rename_column 'issues', 'created_at_ms', 'created_at'
    rename_column 'issues', 'updated_at_ms', 'updated_at'
    add_index 'issues', 'updated_at', :name => 'issues_updated_at'
  end
end


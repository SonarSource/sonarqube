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
# SonarQube 4.4
# SONAR-5329
#
class CreateActivitiesTable < ActiveRecord::Migration
  def self.up
    create_table 'activities' do |t|
      t.column 'created_at',   :datetime, :null => false
      t.column 'user_login', :string, :limit => 255
      t.column 'data_field', :text
      t.column 'log_type', :string, :limit => 50
      t.column 'log_action', :string, :limit => 50
      t.column 'log_message', :string, :limit => 4000
      t.column 'log_key', :string, :limit => 250
    end
    add_index 'activities', :log_key, :name => 'activities_log_key', :unique => true
  end
end

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
class CreateUserRoles < ActiveRecord::Migration
  def self.up
  	create_table :user_roles do |t|
	    t.integer :user_id, :null => true
      t.integer :resource_id, :null => true
      t.string :role, :limit => 64, :null => false
    end
    add_index "user_roles", "user_id", :name => 'user_roles_user'
	  add_index "user_roles", "resource_id", :name => 'user_roles_resource'
  end
end

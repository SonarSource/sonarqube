#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2013 SonarSource
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
# @since Sonar 3.7
# See SONAR-4463
#
class CreatePermissionTemplatesUsers < ActiveRecord::Migration

  def self.up
    create_table :perm_templates_users do |t|
      t.column :user_login,       :string,  :null => false,   :limit => 40
      t.column :template_name,    :string,  :null => false,   :limit => 100
      t.column :created_at,       :datetime,  :null => true
      t.column :updated_at,       :datetime,  :null => true
    end
  end

end
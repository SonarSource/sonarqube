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
# SONAR-6326
#
class CreateProjectProfiles < ActiveRecord::Migration

  def self.up
    create_table :project_qprofiles do |t|
      t.column :project_uuid, :string, :limit => 50, :null => false
      t.column :profile_key, :string, :limit => 255, :null => false
    end

    add_index 'project_qprofiles', ['project_uuid', 'profile_key'], :unique => true, :name => 'uniq_project_qprofiles'
  end

end


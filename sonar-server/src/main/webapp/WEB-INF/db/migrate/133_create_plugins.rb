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
# Sonar 2.3
#
class CreatePlugins < ActiveRecord::Migration

  def self.up
    create_table 'plugins' do |t|
      t.column 'plugin_key', :string, :limit => 100
      t.column 'version', :string, :limit => 100, :null => true
      t.column :name, :string, :limit => 100, :null => true
      t.column :description, :string, :limit => 3000, :null => true
      t.column :organization, :string, :limit => 100, :null => true
      t.column :organization_url, :string, :null => true, :limit => 500
      t.column :license, :string, :limit => 50, :null => true
      t.column :installation_date, :datetime, :null => true
      t.column :plugin_class, :string, :limit => 100, :null => true
      t.column :homepage, :string, :limit => 500, :null => true
      t.column :core, :boolean, :null => true
    end

    create_table 'plugin_files' do |t|
      t.column 'plugin_id', :integer
      t.column 'filename', :string, :limit => 100
    end

    drop_table(:extensions)
    drop_table(:extension_files)
  end

end

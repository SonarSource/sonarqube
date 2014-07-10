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
# Sonar 3.7
# See SONAR-4383
#
class CreateIssueFilters < ActiveRecord::Migration

  def self.up
    create_table :issue_filters do |t|
      t.column :name,        :string,  :null => false,   :limit => 100
      t.column :user_login,  :string,  :null => true,	   :limit => 40
      t.column :shared,      :boolean, :null => false,   :default => false
      t.column :description, :string,  :null => true,    :limit => 4000
      t.column :data,        :text,    :null => true
      t.column :created_at,  :datetime,  :null => true
      t.column :updated_at,  :datetime,  :null => true
    end
    add_index :issue_filters, :name, :name => 'issue_filters_name'
  end

end

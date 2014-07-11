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
# Sonar 3.6
#
class CreateIssueChanges < ActiveRecord::Migration

  def self.up
    create_table :issue_changes do |t|
      t.column :kee,                :string,    :null => true,    :limit => 50
      t.column :issue_key,          :string,    :null => false,   :limit => 50
      t.column :user_login,         :string,    :null => true,	  :limit => 40
      t.column :change_type, 				:string, 	  :null => true,	  :limit => 20
      t.column :change_data,        :text,      :null => true
      t.column :created_at,         :datetime,  :null => true
      t.column :updated_at,         :datetime,  :null => true
    end

    add_index :issue_changes,  :kee,         :name => 'issue_changes_kee'
    add_index :issue_changes,  :issue_key,   :name => 'issue_changes_issue_key'
  end

end


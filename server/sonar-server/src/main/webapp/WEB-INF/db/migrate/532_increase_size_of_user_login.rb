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
# SONAR-5273
#
class IncreaseSizeOfUserLogin < ActiveRecord::Migration

  def self.up
    if dialect()=='sqlserver'
      remove_index :users, :name => 'users_login'
      remove_index :authors, :name => 'uniq_author_logins'
      remove_index :issue_filter_favourites, :name => 'issue_filter_favs_user'
      remove_index :issues, :name => 'issues_assignee'
    end

    change_column 'action_plans', 'user_login', :string, :null => true, :limit => 255
    change_column 'active_rule_changes', 'username', :string, :limit => 255, :null => true
    change_column :authors, 'login', :string, :null => true, :limit => 255
    change_column :issues,  :reporter,             :string,    :null => true,	  :limit => 255
    change_column :issues,  :assignee,             :string,    :null => true,	  :limit => 255
    change_column :issues, :author_login,         :string,    :null => true,    :limit => 255
    change_column :issue_filter_favourites, :user_login, :string, :limit => 255
    change_column :issue_filters, :user_login, :string, :null => true, :limit => 255
    change_column 'manual_measures', :user_login, :string, :null => true, :limit => 255
    change_column :rules, :note_user_login, :string, :null => true, :limit => 255
    change_column :users, :login, :string, :limit => 255, :unique => true

    if dialect()=='sqlserver'
      add_index :users, :login, :name => 'users_login', :unique => true
      add_index :authors, :login, :name => 'uniq_author_logins', :unique => true
      add_index :issue_filter_favourites, :user_login, :name => 'issue_filter_favs_user'
      add_index :issues,  :assignee, :name => 'issues_assignee'
    end
  end
  
end

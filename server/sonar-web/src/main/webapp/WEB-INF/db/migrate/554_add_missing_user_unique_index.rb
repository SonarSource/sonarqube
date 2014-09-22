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
# See SONAR-5405
#
require 'set'

class AddMissingUserUniqueIndex < ActiveRecord::Migration

  class User < ActiveRecord::Base
  end

  class UserRole < ActiveRecord::Base
  end

  class Property < ActiveRecord::Base
    set_table_name 'properties'
  end

  class GroupsUsers < ActiveRecord::Base
    set_table_name 'groups_users'
  end

  class Dashboard < ActiveRecord::Base
    set_table_name 'dashboards'
    has_many :active_dashboards, :dependent => :destroy, :inverse_of => :dashboard
  end

  class ActiveDashboard < ActiveRecord::Base
    set_table_name 'active_dashboards'
    belongs_to :dashboard
  end

  class PermTemplatesUser < ActiveRecord::Base
    set_table_name 'perm_templates_users'
  end

  class MeasureFilter < ActiveRecord::Base
    set_table_name 'measure_filters'
    has_many :measure_filter_favourites, :dependent => :delete_all
  end

  class MeasureFilterFavourite < ActiveRecord::Base
    set_table_name 'measure_filter_favourites'
    belongs_to :measure_filter
  end

  def self.up
    unless index_exists?(:users, :login, nil)
      delete_duplicated_users
      add_index :users, :login, :name => 'users_login', :unique => true
    end
  end

  private
  def self.delete_duplicated_users
    User.reset_column_information
    UserRole.reset_column_information
    Property.reset_column_information
    GroupsUsers.reset_column_information
    Dashboard.reset_column_information
    ActiveDashboard.reset_column_information
    PermTemplatesUser.reset_column_information
    MeasureFilter.reset_column_information
    MeasureFilterFavourite.reset_column_information

    say_with_time 'Delete duplicated users' do
      existing_logins = Set.new
      users=User.find(:all, :select => 'id,login', :order => 'id')
      users.each do |user|
        if existing_logins.include?(user.login)
          say "Delete duplicated login '#{user.login}' (id=#{user.id})"
          UserRole.delete_all(['user_id=?', user.id])
          Property.delete_all(['user_id=?', user.id])
          GroupsUsers.delete_all(['user_id=?', user.id])
          Dashboard.destroy_all(['user_id=?', user.id])
          ActiveDashboard.destroy_all(['user_id=?', user.id])
          PermTemplatesUser.destroy_all(['user_id=?', user.id])
          MeasureFilter.destroy_all(['user_id=?', user.id])
          MeasureFilterFavourite.destroy_all(['user_id=?', user.id])
          user.destroy
        else
          existing_logins.add(user.login)
        end
      end
    end
  end

end

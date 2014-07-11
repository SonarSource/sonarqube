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
# Sonar 4.1
#
class AddUniqueConstraintToGroupsUsers < ActiveRecord::Migration

  class GroupsUser < ActiveRecord::Base
  end

  def self.up
    GroupsUser.reset_column_information

    duplicated_ids = ActiveRecord::Base.connection.select_rows('select user_id,group_id from groups_users group by user_id,group_id having count(*) > 1')
    say_with_time "Remove duplications for #{duplicated_ids.size} user group associations" do
      duplicated_ids.each do |user_id, group_id|
        # delete all rows, then reinsert one
        GroupsUser.delete_all([ "user_id=? and group_id=?", user_id, group_id ])
        ActiveRecord::Base.connection.execute("insert into groups_users (user_id,group_id) values (%s,%s)" % [user_id, group_id])
      end
    end

    add_index :groups_users, [:group_id, :user_id], :name => 'GROUPS_USERS_UNIQUE', :unique => true
  end
end

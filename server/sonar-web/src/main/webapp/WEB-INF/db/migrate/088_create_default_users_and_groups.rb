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
class CreateDefaultUsersAndGroups < ActiveRecord::Migration

  def self.up
  	create_administrators
    create_users
  end

  private
  def self.create_administrators
    administrators=Group.create(:name => 'sonar-administrators', :description => 'System administrators')
    GroupRole.create(:group_id => administrators.id, :role => 'admin')

    admin=User.find_by_login('admin')
    admin.groups<<administrators
    admin.updated_at = Time.now
    admin.send(:update_without_callbacks)
  end

  def self.create_users
    users=Group.create(:name => 'sonar-users', :description => 'Any new users created will automatically join this group')

    # The user 'admin' is considered as a user
    admin=User.find_by_login('admin')
    admin.groups<<users
    admin.updated_at = Time.now
    admin.send(:update_without_callbacks)
  end
end

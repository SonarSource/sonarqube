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
# SonarQube 4.1
# SONAR-2447
#

class AddAdministerIssuesPerm < ActiveRecord::Migration

  class GroupRole < ActiveRecord::Base
  end

  class UserRole < ActiveRecord::Base
  end

  def self.up
    group_roles=GroupRole.find(:all, :conditions => {:role => 'user'})
    group_roles.each do |group_role|
      GroupRole.create(:group_id => group_role.group_id, :role => 'issueadmin', :resource_id => group_role.resource_id)
    end

    user_roles=UserRole.find(:all, :conditions => {:role => 'user'})
    user_roles.each do |user_role|
      UserRole.create(:user_id => user_role.user_id, :role=> 'issueadmin', :resource_id => user_role.resource_id)
    end
  end

end

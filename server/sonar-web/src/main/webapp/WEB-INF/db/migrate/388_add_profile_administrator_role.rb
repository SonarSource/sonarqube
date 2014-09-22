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

class AddProfileAdministratorRole < ActiveRecord::Migration

  class GroupRole < ActiveRecord::Base
  end

  class UserRole < ActiveRecord::Base
  end

  def self.up
    group_roles=GroupRole.find(:all, :conditions => {:role => 'admin', :resource_id => nil})
    groups = group_roles.map { |ur| ur.group_id }
    groups.each do |group_id|
      GroupRole.create(:group_id => group_id, :role => 'profileadmin', :resource_id => nil)
    end

    user_roles=UserRole.find(:all, :conditions => {:role => 'admin', :resource_id => nil})
    users = user_roles.map { |ur| ur.user_id }
    users.each do |user_id|
      UserRole.create(:user_id => user_id, :role=> 'profileadmin', :resource_id => nil)
    end
  end

end

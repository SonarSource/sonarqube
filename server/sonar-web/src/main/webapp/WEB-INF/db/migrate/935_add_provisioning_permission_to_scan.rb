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
# SONAR-6861
#
# Add the permission "provisioning" to the users and groups that have already
# the permission "scan" and if, and only if, the property sonar.preventAutoProjectCreation is false
# (default value)
class AddProvisioningPermissionToScan < ActiveRecord::Migration

  class Property < ActiveRecord::Base
    set_table_name 'properties'
  end

  class GroupRole < ActiveRecord::Base
  end

  class UserRole < ActiveRecord::Base
  end

  def self.up
    papc = Property.find(:first, :conditions => ['prop_key=?', 'sonar.preventAutoProjectCreation'])
    unless papc && papc.text_value=='true'
      group_roles=GroupRole.find(:all, :conditions => {:role => 'scan', :resource_id => nil})
      groups = group_roles.map { |ur| ur.group_id }
      groups.each do |group_id|
        unless GroupRole.exists?(:group_id => group_id, :role => 'provisioning', :resource_id => nil)
          GroupRole.create(:group_id => group_id, :role => 'provisioning', :resource_id => nil)
        end
      end

      user_roles=UserRole.find(:all, :conditions => {:role => 'scan', :resource_id => nil})
      users = user_roles.map { |ur| ur.user_id }
      users.each do |user_id|
        unless UserRole.exists?(:user_id => user_id, :role => 'provisioning', :resource_id => nil)
          UserRole.create(:user_id => user_id, :role=> 'provisioning', :resource_id => nil)
        end
      end
     end
  end

end

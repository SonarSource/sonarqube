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
# Sonar 3.7
# SONAR-4397
#

class AddScanAndDryRunPermissions < ActiveRecord::Migration

  def self.up
    # -- Role scan --
    group_roles=GroupRole.find(:all, :conditions => {:role => 'admin', :resource_id => nil})
    groups = group_roles.map { |ur| ur.group_id }
    # Anyone
    unless groups.include?(nil)
      groups << nil
    end
    groups.each do |group_id|
      GroupRole.create(:group_id => group_id, :role => 'scan', :resource_id => nil)
    end

    user_roles=UserRole.find(:all, :conditions => {:role => 'admin', :resource_id => nil})
    users = user_roles.map { |ur| ur.user_id }
    users.each do |user_id|
      UserRole.create(:user_id => user_id, :role=> 'scan', :resource_id => nil)
    end
    
    # -- Role dryrun --
    group_roles=GroupRole.find(:all, :conditions => {:role => 'admin', :resource_id => nil})
    groups = group_roles.map { |ur| ur.group_id }
    # Anyone
    unless groups.include?(nil)
      groups << nil
    end
    # sonar-users
    userGroupName = Property.by_key('sonar.defaultGroup')
    userGroupName = 'sonar-users' if userGroupName.nil?
    userGroup = Group.find(:all, :conditions => {:name => userGroupName}).first
    unless userGroup.nil? || groups.include?(userGroup.id)
      groups << userGroup.id
    end
    
    groups.each do |group_id|
      GroupRole.create(:group_id => group_id, :role => 'dryrun', :resource_id => nil)
    end

    user_roles=UserRole.find(:all, :conditions => {:role => 'admin', :resource_id => nil})
    users = user_roles.map { |ur| ur.user_id }
    users.each do |user_id|
      UserRole.create(:user_id => user_id, :role=> 'dryrun', :resource_id => nil)
    end
    
  end

end

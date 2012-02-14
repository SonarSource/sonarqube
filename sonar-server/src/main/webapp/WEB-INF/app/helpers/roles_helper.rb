#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
module RolesHelper
  
  def users(role, resource_id=nil)
    resource_id=(resource_id.blank? ? nil : resource_id.to_i)
    user_roles=UserRole.find(:all, :conditions => {:role => role, :resource_id => resource_id})
    user_roles.map {|ur| ur.user}.sort
  end

  def all_users
    User.find(:all, :conditions => ["active=?", true], :order => 'name')
  end

  def groups(role, resource_id=nil)
    resource_id=(resource_id.blank? ? nil : resource_id.to_i)
    group_roles=GroupRole.find(:all, :conditions => {:role => role, :resource_id => resource_id})
    group_roles.map{|ur| ur.group}.sort do |x,y|
      x ? x<=>y : -1
    end
  end

  def all_groups
    [nil].concat(Group.find(:all, :order => 'name'))
  end

  def group_name(group)
    group ? group.name : 'Anyone'
  end

  def role_name(role)
    case(role.to_s)
      when 'admin': 'Administrators'
      when 'default-admin': 'Administrators'
      when 'user': 'Users'
      when 'default-user': 'Users'
      when 'codeviewer': 'Code viewers'
      when 'default-codeviewer': 'Code viewers'
      else role.to_s
    end
  end
end

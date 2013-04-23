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
module RolesHelper

  def users(role, resource_id=nil)
    resource_id=(resource_id.blank? ? nil : resource_id.to_i)
    user_roles=UserRole.find(:all, :include => 'user', :conditions => {:role => role, :resource_id => resource_id, :users => {:active => true}})
    users = user_roles.map { |ur| ur.user }
    Api::Utils.insensitive_sort(users) { |user| user.name }
  end

  def all_users
    users = User.find(:all, :conditions => ["active=?", true])
    Api::Utils.insensitive_sort(users) { |user| user.name }
  end

  def groups(role, resource_id=nil)
    resource_id=(resource_id.blank? ? nil : resource_id.to_i)
    group_roles=GroupRole.find(:all, :include => 'group', :conditions => {:role => role, :resource_id => resource_id})
    groups = group_roles.map { |ur| ur.group }
    Api::Utils.insensitive_sort(groups) { |group| group ? group.name : '' }
  end

  def all_groups
    [nil].concat(Api::Utils.insensitive_sort(Group.all) { |group| group.name })
  end

  def group_name(group)
    group ? group.name : 'Anyone'
  end

  def default_project_group_names(role, qualifier)
    group_names=(controller.java_facade.getConfigurationValue("sonar.role.#{role}.#{qualifier}.defaultGroups")||'').split(',')

    # verify that groups still exist
    result = []
    if group_names.size>0
      groups = Group.find(:all, :conditions => ['name in (?)', group_names])
      result = Api::Utils.insensitive_sort(groups.map{|g| g.name})
      result = ['Anyone'].concat(result) if group_names.include? 'Anyone'
    end
    result
  end

  def default_project_users(role, qualifier)
    logins=(controller.java_facade.getConfigurationValue("sonar.role.#{role}.#{qualifier}.defaultUsers") || '').split(',')
    users = User.find(:all, :conditions => ['login in (?) and active=?', logins, true])
    Api::Utils.insensitive_sort(users) { |user| user.name }
  end

  def role_name(role)
    case (role.to_s)
      when 'admin' then
        'Administrators'
      when 'user' then
        'Users'
      when 'codeviewer' then
        'Code Viewers'
      else
        role.to_s
    end
  end

  def global_role_name(role)
    message("global_role.#{role}", :default => role)
  end

end

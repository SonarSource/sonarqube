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
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

class SonarAuthorizer

  def has_role?(user, role)
    global_roles(user).include?(role)
  end

  def has_role_for_resources?(user, role, resource_ids)
    return [] if resource_ids.empty?

    compacted_resource_ids=resource_ids.compact
    group_ids=user.groups.map(&:id)

    # Oracle is limited to 1000 elements in clause "IN"
    page_size=999
    page_count=(compacted_resource_ids.size/page_size)
    page_count+=1 if (compacted_resource_ids.size % page_size)>0

    sanitized_role = ActiveRecord::Base.connection.quote_string(role.to_s)

    group_roles=[]
    if group_ids.empty?
      # Some databases do not support empty IN
      page_count.times do |page_index|
        page_rids=compacted_resource_ids[page_index*page_size...(page_index+1)*page_size]
        group_roles.concat(
          ActiveRecord::Base.connection.execute("SELECT resource_id FROM group_roles WHERE role='#{sanitized_role}' and group_id is null and resource_id in (#{page_rids.join(',')})")
        )
      end
    else
      page_count.times do |page_index|
        page_rids=compacted_resource_ids[page_index*page_size...(page_index+1)*page_size]
        group_roles.concat(
          ActiveRecord::Base.connection.execute("SELECT resource_id FROM group_roles WHERE role='#{sanitized_role}' and (group_id is null or group_id in(#{group_ids.join(',')})) and resource_id in (#{page_rids.join(',')})")
        )
      end
    end

    user_roles=[]
    if user.id
      page_count.times do |page_index|
        page_rids=compacted_resource_ids[page_index*page_size...(page_index+1)*page_size]
        user_roles.concat(
          ActiveRecord::Base.connection.execute("SELECT resource_id FROM user_roles WHERE role='#{sanitized_role}' and user_id=#{user.id} and resource_id in (#{page_rids.join(',')})")
        )
      end
    end

    authorized_resource_ids={}
    (group_roles.concat(user_roles)).each do |x|
      authorized_resource_ids[x['resource_id'].to_i]=true
    end

    result=Array.new(resource_ids.size)
    resource_ids.each_with_index do |rid,index|
      result[index]=((authorized_resource_ids[rid]) || false)
    end
    result
  end

  def on_logout(user)
    # nothing
  end

  private

  def global_roles(user)
    group_ids=user.groups.map(&:id)
    if group_ids.empty?
      # Some databases do not support empty IN
      global_group_roles=GroupRole.all(:select => 'role', :conditions => ["resource_id is null and group_id is null"]).map{|gr| gr.role.to_sym}
    else
      global_group_roles=GroupRole.all(:select => 'role', :conditions => ["resource_id is null and (group_id is null or group_id in(?))", group_ids]).map{|gr| gr.role.to_sym}
    end
    global_user_roles=user.user_roles.select{|ur| ur.resource_id.nil?}.map{|ur| ur.role.to_sym}

    global_roles=(global_group_roles.concat(global_user_roles))
    global_roles
  end
  
end

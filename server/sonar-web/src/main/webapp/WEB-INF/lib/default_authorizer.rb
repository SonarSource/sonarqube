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

class DefaultAuthorizer

  def has_role?(user, role)
    global_roles(user).include?(role)
  end

  def has_role_for_resources?(user, role, component_uuids)
    return [] if component_uuids.empty?

    compacted_component_uuids=component_uuids.compact
    group_ids=user.groups.map(&:id)

    # Oracle is limited to 1000 elements in clause "IN"
    page_size=999
    page_count=(compacted_component_uuids.size/page_size)
    page_count+=1 if (compacted_component_uuids.size % page_size)>0

    sanitized_role = ActiveRecord::Base.connection.quote_string(role.to_s)

    group_roles=[]
    if group_ids.empty?
      # Some databases do not support empty IN
      page_count.times do |page_index|
        page_component_uuids=compacted_component_uuids[page_index*page_size...(page_index+1)*page_size]
        group_roles.concat(
          ActiveRecord::Base.connection.execute("SELECT p.uuid FROM group_roles gr INNER JOIN projects p ON p.id=gr.resource_id WHERE gr.role='#{sanitized_role}' and gr.group_id is null and p.uuid in (#{page_component_uuids.map{ |u| "'#{u}'" }.join(',')})")
        )
      end
    else
      compacted_group_ids=group_ids.compact
      gr_page_count=(compacted_group_ids.size/page_size)
      gr_page_count+=1 if (compacted_group_ids.size % page_size)>0

      page_count.times do |page_index|
        page_component_uuids=compacted_component_uuids[page_index*page_size...(page_index+1)*page_size]
        gr_page_count.times do |gr_page_index|
          page_grids=compacted_group_ids[gr_page_index*page_size...(gr_page_index+1)*page_size]
          group_roles.concat(
            ActiveRecord::Base.connection.execute("SELECT p.uuid FROM group_roles gr INNER JOIN projects p ON p.id=gr.resource_id WHERE gr.role='#{sanitized_role}' and (gr.group_id is null or gr.group_id in(#{page_grids.join(',')})) and p.uuid in (#{page_component_uuids.map{ |u| "'#{u}'" }.join(',')})")
          )
        end
      end
    end

    user_roles=[]
    if user.id
      page_count.times do |page_index|
        page_component_uuids=compacted_component_uuids[page_index*page_size...(page_index+1)*page_size]
        user_roles.concat(
          ActiveRecord::Base.connection.execute("SELECT p.uuid FROM user_roles ur INNER JOIN projects p ON p.id=ur.resource_id WHERE ur.role='#{sanitized_role}' and ur.user_id=#{user.id} and p.uuid in (#{page_component_uuids.map{ |u| "'#{u}'" }.join(',')})")
        )
      end
    end

    authorized_component_uuids={}
    (group_roles.concat(user_roles)).each do |x|
      authorized_component_uuids[x['uuid']]=true
    end

    result=Array.new(component_uuids.size)
    component_uuids.each_with_index do |uuid,index|
      result[index]=((authorized_component_uuids[uuid]) || false)
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
      # Oracle is limited to 1000 elements in clause "IN"
      compacted_group_ids=group_ids.compact
      page_size=999
      page_count=(compacted_group_ids.size/page_size)
      page_count+=1 if (compacted_group_ids.size % page_size)>0
      global_group_roles=[]
      page_count.times do |page_index|
        page_ids=compacted_group_ids[page_index*page_size...(page_index+1)*page_size]
        global_group_roles.concat(
          GroupRole.all(:select => 'role', :conditions => ["resource_id is null and (group_id is null or group_id in(?))", page_ids]).map{|gr| gr.role.to_sym}
        )
      end
    end
    global_user_roles=UserRole.all(:select => 'role', :conditions => ["user_id=? and resource_id is null", user.id]).map{|ur| ur.role.to_sym}

    global_roles=(global_group_roles.concat(global_user_roles))
    global_roles
  end
  
end

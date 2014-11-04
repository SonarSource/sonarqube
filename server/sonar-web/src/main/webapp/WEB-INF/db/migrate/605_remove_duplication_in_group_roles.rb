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
# SonarQube 4.5.2
# SONAR-4950
#
class RemoveDuplicationInGroupRoles < ActiveRecord::Migration

  class GroupRole < ActiveRecord::Base
  end

  def self.up
    GroupRole.reset_column_information

    duplicated_ids = ActiveRecord::Base.connection.select_rows('select group_id,resource_id,role from group_roles group by group_id,resource_id,role having count(*) > 1')
    say_with_time "Remove #{duplicated_ids.size} duplicated group roles" do
      duplicated_ids.each do |fields|
        rows = GroupRole.find(:all, :conditions => {:group_id => fields[0], :resource_id => fields[1], :role => fields[2]})
        # delete all rows except the last one
        rows[0...-1].each do |row|
          GroupRole.delete(row.id)
        end
      end
    end

    add_index 'group_roles', ['group_id', 'resource_id', 'role'], :unique => true, :name => 'uniq_group_roles'
  end
end

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
# Sonar 2.13
#
class CreateActionPlans < ActiveRecord::Migration

  def self.up
    create_table 'action_plans' do |t|
      t.timestamps
      t.column 'name',          :string,      :null => true,    :limit => 200
      t.column 'description',   :string,      :null => true,    :limit => 1000
      t.column 'deadline',     :datetime,    :null => true
      t.column 'user_login',    :string,      :null => true,    :limit => 40
      t.column 'project_id',    :integer,     :null => true
      t.column 'status',        :string,      :null => true,    :limit => 10
      t.column 'kee',           :string,      :null => true,    :limit => 100
    end
    alter_to_big_primary_key('action_plans')

    add_index "action_plans", "project_id", :name => 'I_ACTION_PLANS_PROJECT_ID'

  end

end

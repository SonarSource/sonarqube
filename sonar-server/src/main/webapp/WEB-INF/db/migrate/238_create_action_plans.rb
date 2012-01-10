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

#
# Sonar 2.13
#
class CreateActionPlans < ActiveRecord::Migration

  def self.up
    create_table 'action_plans' do |t|
      t.timestamps
      t.column 'name',          :string,      :null => true,    :limit => 200
      t.column 'description',   :string,      :null => true,    :limit => 1000
      t.column 'dead_line',     :datetime,    :null => true
      t.column 'user_login',    :string,      :null => true,    :limit => 40
      t.column 'project_id',    :integer,     :null => true
      t.column 'status',        :string,      :null => true,    :limit => 10
    end
    alter_to_big_primary_key('action_plans')
    add_index "action_plans", "project_id"
    
    create_table :action_plans_reviews, :id => false do |t|
      t.integer :action_plan_id
      t.integer :review_id
    end    
    add_index "action_plans_reviews", "action_plan_id"
    add_index "action_plans_reviews", "review_id"
    
  end

end

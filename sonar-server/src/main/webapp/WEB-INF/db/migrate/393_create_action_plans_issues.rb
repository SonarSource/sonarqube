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
# Sonar 3.6
#
class CreateActionPlansIssues < ActiveRecord::Migration

  def self.up
    create_table :action_plans_issues, :id => false do |t|
      t.integer :action_plan_id
      t.integer :issue_id
    end

    add_index "action_plans_issues", "action_plan_id", :name => 'I_ACT_PLA_ISSUE_ACT_PLA_ID'
    add_index "action_plans_issues", "issue_id", :name => 'I_ACT_PLA_ISSUE_ISSUE_ID'
  end

end


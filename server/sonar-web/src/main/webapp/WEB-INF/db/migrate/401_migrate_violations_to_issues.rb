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
# Sonar 3.6
# See SONAR-4305
#
class MigrateViolationsToIssues < ActiveRecord::Migration

  def self.up
    add_index :issues,  :kee,                 :name => 'issues_kee',         :unique => true
    add_index :issues,  :component_id,        :name => 'issues_component_id'
    add_index :issues,  :root_component_id,   :name => 'issues_root_component_id'
    add_index :issues,  :rule_id,             :name => 'issues_rule_id'
    add_index :issues,  :severity,            :name => 'issues_severity'
    add_index :issues,  :status,              :name => 'issues_status'
    add_index :issues,  :resolution,          :name => 'issues_resolution'
    add_index :issues,  :assignee,            :name => 'issues_assignee'
    add_index :issues,  :action_plan_key,     :name => 'issues_action_plan_key'
    add_index :issues,  :issue_creation_date, :name => 'issues_creation_date'
  end

end

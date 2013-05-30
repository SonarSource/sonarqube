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

class ActionPlan

  def self.to_hash(action_plan)
    hash = {:key => action_plan.key(), :name => action_plan.name(), :status => action_plan.status()}
    hash[:project] = action_plan.projectKey() if action_plan.projectKey() && !action_plan.projectKey().blank?
    hash[:desc] = action_plan.description() if action_plan.description() && !action_plan.description().blank?
    hash[:userLogin] = action_plan.userLogin() if action_plan.userLogin()
    hash[:deadLine] = Api::Utils.format_datetime(action_plan.deadLine()) if action_plan.deadLine()
    hash[:totalIssues] = action_plan.totalIssues() if action_plan.respond_to?('totalIssues')
    hash[:unresolvedIssues] = action_plan.unresolvedIssues() if action_plan.respond_to?('unresolvedIssues')
    hash[:createdAt] = Api::Utils.format_datetime(action_plan.createdAt()) if action_plan.createdAt()
    hash[:updatedAt] = Api::Utils.format_datetime(action_plan.updatedAt()) if action_plan.updatedAt()
    hash
  end

end
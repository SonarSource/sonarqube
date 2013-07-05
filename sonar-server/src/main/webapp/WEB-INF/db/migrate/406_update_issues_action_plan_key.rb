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
# Sonar 3.6.1
#
# SONAR-4449
#
class UpdateIssuesActionPlanKey < ActiveRecord::Migration

  class Issue < ActiveRecord::Base
  end

  # Update issues that are still linked to removed action plans
  def self.up
    Issue.reset_column_information
    Issue.update_all({:action_plan_key => nil}, "action_plan_key is not null AND not exists (select * from action_plans ap where ap.kee = action_plan_key)")
  end

end


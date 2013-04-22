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
# Sonar 2.13
# http://jira.codehaus.org/browse/SONAR-1974
#
class RefactorRuleMeasures < ActiveRecord::Migration

  class ProjectMeasure < ActiveRecord::Base
  end

  class Metric < ActiveRecord::Base
  end

  def self.up
    Metric.reset_column_information
    ProjectMeasure.reset_column_information

    replace('violations', 0, 'info_violations')
    replace('violations', 1, 'minor_violations')
    replace('violations', 2, 'major_violations')
    replace('violations', 3, 'critical_violations')
    replace('violations', 4, 'blocker_violations')

    replace('new_violations', 0, 'new_info_violations')
    replace('new_violations', 1, 'new_minor_violations')
    replace('new_violations', 2, 'new_major_violations')
    replace('new_violations', 3, 'new_critical_violations')
    replace('new_violations', 4, 'new_blocker_violations')
  end


  private
  def self.replace(from_metric_key, from_severity, to_metric_key)
    from_metric = Metric.find_by_name(from_metric_key)
    to_metric = Metric.find_by_name(to_metric_key)

    if from_metric && to_metric
      say_with_time("Update metric #{to_metric_key}") do
        ProjectMeasure.update_all("metric_id=#{to_metric.id}", "metric_id=#{from_metric.id} AND rule_id IS NOT NULL AND rule_priority=#{from_severity}")
      end
    end
  end
end

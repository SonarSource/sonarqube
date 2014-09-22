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
# SonarQube 4.3
# SONAR-4996
#
class UpdateConditionsOnDebtToMinutes < ActiveRecord::Migration

  class Property < ActiveRecord::Base

  end

  class QualityGateCondition < ActiveRecord::Base

  end

  class Metric < ActiveRecord::Base

  end

  def self.up
    Property.reset_column_information
    Metric.reset_column_information
    QualityGateCondition.reset_column_information

    hours_in_day_prop = Property.find_by_prop_key('sonar.technicalDebt.hoursInDay')
    hours_in_day = hours_in_day_prop && hours_in_day_prop.text_value ? hours_in_day_prop.text_value.to_i : 8

    metrics = Metric.find(:all, :conditions => ['name in (?)', ['sqale_index', 'new_technical_debt',
                                                                'sqale_effort_to_grade_a', 'sqale_effort_to_grade_b', 'sqale_effort_to_grade_c', 'sqale_effort_to_grade_d',
                                                                'blocker_remediation_cost', 'critical_remediation_cost', 'major_remediation_cost', 'minor_remediation_cost',
                                                                'info_remediation_cost']])
    conditions = QualityGateCondition.all(:conditions => ['metric_id in (?)', metrics.map { |m| m.id } ])

    conditions.each do |condition|
      condition.value_error = convert_days_to_minutes(condition.value_error.to_f, hours_in_day) unless condition.value_error.blank?
      condition.value_warning = convert_days_to_minutes(condition.value_warning.to_f, hours_in_day) unless condition.value_warning.blank?
      condition.save!
    end
  end

  def self.convert_days_to_minutes(hours, hours_in_day)
    result = hours * hours_in_day * 60
    # Round value
    result.ceil
  end
end

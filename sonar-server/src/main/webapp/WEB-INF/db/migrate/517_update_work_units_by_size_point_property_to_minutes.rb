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
class UpdateWorkUnitsBySizePointPropertyToMinutes < ActiveRecord::Migration

  class Property < ActiveRecord::Base

  end

  def self.up
    hours_in_day_prop = Property.find_by_prop_key('sonar.technicalDebt.hoursInDay')
    hours_in_day = hours_in_day_prop && hours_in_day_prop.text_value ? hours_in_day_prop.text_value.to_i : 8
    work_units_by_size_point = Property.find_by_prop_key('workUnitsBySizePoint')
    if work_units_by_size_point && work_units_by_size_point.text_value && work_units_by_size_point.text_value.to_f
      work_units_by_size_point.text_value = convert_days_to_minutes(work_units_by_size_point.text_value.to_f, hours_in_day).to_s
      work_units_by_size_point.save!
    end

    language_specific_parameters = Property.find_by_prop_key('languageSpecificParameters')
    if language_specific_parameters
      values = language_specific_parameters.text_value.split(',')
      values.each do |value|
        prop = Property.find_by_prop_key('languageSpecificParameters.' + value + '.man_days')
        if prop && prop.text_value && prop.text_value.to_f
          prop.text_value = convert_days_to_minutes(prop.text_value.to_f, hours_in_day).to_s
          prop.save!
        end
      end
    end
  end

  def self.convert_days_to_minutes(hours, hours_in_day)
    result = hours * hours_in_day * 60
    # Round value
    result.ceil
  end
end

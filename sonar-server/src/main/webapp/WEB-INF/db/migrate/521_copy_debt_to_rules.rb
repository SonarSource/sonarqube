#
# SonarQube, open source software quality management tool.
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
# Sonar 4.3
# SONAR-5056
#
class CopyDebtToRules < ActiveRecord::Migration

  class Characteristic < ActiveRecord::Base
  end

  class Rule < ActiveRecord::Base
  end

  def self.up
    Rule.reset_column_information

    requirements = Characteristic.all(
        :conditions => ['rule_id IS NOT NULL AND function_key IS NOT NULL AND enabled=?', true]
    )
    requirements.each do |requirement|
      rule = Rule.find_by_id(requirement.rule_id)
      if rule
        rule.characteristic_id = requirement.parent_id
        # functions are now store in upper case
        rule.remediation_function = requirement.function_key.upcase
        rule.remediation_factor = to_new_remediation(requirement.factor_value, requirement.factor_unit)
        rule.remediation_offset = to_new_remediation(requirement.offset_value, requirement.offset_unit)
        rule.save
      end
    end
  end

  def self.to_new_remediation(old_value, old_unit)
    if old_value
      unit = old_unit || 'd'
      unit = unit == 'mn' ? 'min' : unit
      # As value is stored in double, we have to round it in order to have an integer (for instance, if it was 1.6, we'll use 2)
      old_value.to_f.ceil.to_s + unit
    else
      '0d'
    end
  end

end


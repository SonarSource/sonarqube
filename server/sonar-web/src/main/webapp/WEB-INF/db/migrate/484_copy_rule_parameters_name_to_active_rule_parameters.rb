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
# SonarQube 4.2
# SONAR-4950
#
class CopyRuleParametersNameToActiveRuleParameters < ActiveRecord::Migration

  class ActiveRuleParameter < ActiveRecord::Base
  end

  class RulesParameter < ActiveRecord::Base
  end

  def self.up
    ActiveRuleParameter.reset_column_information

    rule_params_by_id = {}
    RulesParameter.all.each do |rule_param|
      rule_params_by_id[rule_param.id] = rule_param
    end

    ActiveRuleParameter.all.each do |active_rule_parameter|
      rule_param = rule_params_by_id[active_rule_parameter.rules_parameter_id]
      active_rule_parameter.rules_parameter_key = rule_param.name if rule_param
      active_rule_parameter.save
    end
  end
end

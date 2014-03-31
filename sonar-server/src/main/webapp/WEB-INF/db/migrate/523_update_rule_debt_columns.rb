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
#
# TO BE DELETED WHEN EXECUTED ON DORY
#
class UpdateRuleDebtColumns < ActiveRecord::Migration

  class Rule < ActiveRecord::Base
  end

  def self.up
    Metric.reset_column_information

    rename_column(:rules, :remediation_factor, :remediation_coeff)
    rename_column(:rules, :default_remediation_factor, :default_remediation_coeff)

    # As CopyRequirementsFromCharacteristicsToRules has been updated to not insert 0X durations, we have update columns to reflect what should have been done.

    Rule.update_all("remediation_coeff=NULL", "remediation_coeff = '0d'" )
    Rule.update_all("remediation_coeff=NULL", "remediation_coeff = '0h'" )
    Rule.update_all("remediation_coeff=NULL", "remediation_coeff = '0min'" )
    Rule.update_all("remediation_coeff=NULL", "remediation_coeff = '0mn'" )

    Rule.update_all("remediation_offset=NULL", "remediation_offset = '0d'" )
    Rule.update_all("remediation_offset=NULL", "remediation_offset = '0h'" )
    Rule.update_all("remediation_offset=NULL", "remediation_offset = '0min'" )
    Rule.update_all("remediation_offset=NULL", "remediation_offset = '0mn'" )
  end

end

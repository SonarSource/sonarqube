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
# SonarQube 5.5
# SONAR-7453
#
class RenameRulesColumns < ActiveRecord::Migration

  def self.up
    rename_column 'rules', 'effort_to_fix_description', 'gap_description'
    rename_column 'rules', 'default_remediation_function', 'def_remediation_function'
    rename_column 'rules', 'remediation_coeff', 'remediation_gap_mult'
    rename_column 'rules', 'default_remediation_coeff', 'def_remediation_gap_mult'
    rename_column 'rules', 'remediation_offset', 'remediation_base_effort'
    rename_column 'rules', 'default_remediation_offset', 'def_remediation_base_effort'
  end

end

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
# SONAR-5056
#
class AddDebtColumnsToRules < ActiveRecord::Migration

  def self.up
    add_column 'rules', :characteristic_id,             :integer,   :null => true
    add_column 'rules', :default_characteristic_id,     :integer,   :null => true
    add_column 'rules', :remediation_function,          :string,    :null => true,   :limit => 20
    add_column 'rules', :default_remediation_function,  :string,    :null => true,   :limit => 20
    add_column 'rules', :remediation_coeff,             :string,    :null => true,   :limit => 20
    add_column 'rules', :default_remediation_coeff,     :string,    :null => true,   :limit => 20
    add_column 'rules', :remediation_offset,            :string,    :null => true,   :limit => 20
    add_column 'rules', :default_remediation_offset,    :string,    :null => true,   :limit => 20
    add_column 'rules', :effort_to_fix_description,     :string,    :null => true,   :limit => 4000
    end

end


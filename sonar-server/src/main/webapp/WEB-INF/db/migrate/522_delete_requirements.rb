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
class DeleteRequirements < ActiveRecord::Migration

  class Characteristic < ActiveRecord::Base
  end

  def self.up
    Characteristic.reset_column_information

    Characteristic.delete_all('rule_id IS NOT NULL')

    # Remove columns on debt
    remove_column('characteristics', 'root_id')
    remove_column('characteristics', 'rule_id')
    remove_column('characteristics', 'function_key')
    remove_column('characteristics', 'factor_value')
    remove_column('characteristics', 'factor_unit')
    remove_column('characteristics', 'offset_value')
    remove_column('characteristics', 'offset_unit')
  end

end


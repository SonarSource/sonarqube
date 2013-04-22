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
# Sonar 2.10
#
class SetNullableRuleName < ActiveRecord::Migration

  class Rule < ActiveRecord::Base
  end

  def self.up
    add_column(:rules, :temp_name, :string, :limit => 200, :null => true)

    Rule.reset_column_information
    Rule.update_all('temp_name=name')

    remove_column(:rules, :name)
    rename_column(:rules, :temp_name, :name)
  end

end

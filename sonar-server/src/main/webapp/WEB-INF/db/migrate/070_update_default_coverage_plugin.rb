#
# Sonar, entreprise quality control tool.
# Copyright (C) 2009 SonarSource SA
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
class UpdateDefaultCoveragePlugin < ActiveRecord::Migration

  def self.up
    property=Property070.find(:first, :conditions => ['prop_key=?','sonar.core.codeCoveragePlugin'])
    if property and property.prop_value=='clover,cobertura'
      property.prop_value='cobertura'
      property.save!
    end
  end

  def self.down

  end

  private

  class Property070 < ActiveRecord::Base
    set_table_name 'properties'
    self.primary_key = 'prop_key'
  end

end
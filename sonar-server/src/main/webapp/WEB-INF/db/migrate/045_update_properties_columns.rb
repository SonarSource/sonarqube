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
class UpdatePropertiesColumns < ActiveRecord::Migration

  def self.up
    p=Property045.find(:first, :conditions => {:prop_key => 'tendency.depth'})
    previous_tendency_value=(p ? p.prop_value : nil)

    remove_column :properties, :prop_value
    add_column :properties, :prop_value, :text, :null => true
    Property045.reset_column_information

    tendency=Property045.find(:first, :conditions => {:prop_key => 'tendency.depth'})
    if tendency and previous_tendency_value
      tendency.prop_value=previous_tendency_value
      tendency.save!
    end

    Property.reset_column_information
  end

  def self.down
    
  end


  
end

class Property045 < ActiveRecord::Base
  set_table_name 'properties'
  self.primary_key = 'prop_key'
end

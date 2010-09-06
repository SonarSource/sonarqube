#
# Sonar, open source software quality management tool.
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
class AddPropertiesResource < ActiveRecord::Migration

  def self.up
    properties=Property059.find(:all)

    drop_table 'properties'

    create_table 'properties' do |t|
      t.column :prop_key,   :string, :limit => 512
      t.column :resource_id, :integer, :null => true
	    t.column :prop_value, :string, :limit => 4000
    end
    add_index :properties, :prop_key, :name => 'properties_key'

    Property.reset_column_information

    properties.each do |p|
      Property.create(:prop_key => p.prop_key, :prop_value => p.prop_value)
    end

  end

  def self.down

  end


  class Property059 < ActiveRecord::Base
    set_table_name 'properties'
    self.primary_key = 'prop_key'
  end
end

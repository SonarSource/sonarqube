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
# Sonar 2.10
#
class CreateManualMeasures < ActiveRecord::Migration

  def self.up
    create_table 'manual_measures' do |t|
      t.column 'metric_id', :integer, :null => false
      t.column 'resource_id', :integer, :null => true
      t.column 'value', :decimal,   :null => true, :precision => 30, :scale => 20
      t.column 'text_value', :string, :null => true, :limit => 4000
      t.column 'user_login', :string, :null => true, :limit => 40
      t.column 'description', :string, :null => true, :limit => 4000
      t.timestamps
    end
    alter_to_big_primary_key('manual_measures')
    add_index 'manual_measures', 'resource_id', :name => 'manual_measures_resource_id'
  end

end

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
# Sonar 3.4
#
class CreateMeasureFilters < ActiveRecord::Migration
  def self.up
    create_table 'measure_filters' do |t|
      t.column 'name', :string, :null => false, :limit => 100
      t.column 'user_id', :integer, :null => true
      t.column 'shared', :boolean, :default => false, :null => false
      t.column 'description', :string, :null => true, :limit => 4000
      t.column 'data', :text, :null => true
      t.timestamps
    end
    add_index 'measure_filters', 'name', :name => 'measure_filters_name'
  end
end


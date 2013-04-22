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
# Sonar 2.2
#
class CreateFilters < ActiveRecord::Migration

  def self.up
    create_table 'filters' do |t|
      t.column 'name', :string, :limit => 100
      t.column 'user_id', :integer, :null => true
      t.column 'shared', :boolean, :null => true
      t.column 'favourites', :boolean, :null => true
      t.column 'resource_id', :integer, :null => true
      t.column 'default_view', :string, :limit => 20, :null => true
      t.column 'page_size', :integer, :null => true
      t.column 'kee', :string, :limit => 100, :null => true
    end

    create_table 'filter_columns' do |t|
      t.column 'filter_id', :integer
      t.column 'family', :string, :limit => 100, :null => true
      t.column 'kee', :string, :limit => 100, :null => true
      t.column 'sort_direction', :string, :limit => 5, :null => true
      t.column 'order_index', :integer, :null => true
    end

    create_table 'criteria' do |t|
      t.column 'filter_id', :integer
      t.column 'family', :string, :limit => 100, :null => true
      t.column 'kee', :string, :limit => 100, :null => true
      t.column 'operator', :string, :limit => 20, :null => true
      t.column 'value', :decimal, :null => true, :precision => 30, :scale => 20
      t.column 'text_value', :string, :null => 200, :null => true
    end

    create_table 'active_filters' do |t|
      t.column 'filter_id', :integer
      t.column 'user_id', :integer, :null => true
      t.column 'order_index', :integer, :null => true
    end

  end

end
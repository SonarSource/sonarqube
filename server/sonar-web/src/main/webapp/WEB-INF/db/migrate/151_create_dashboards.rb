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
# Sonar 2.4
#
class CreateDashboards < ActiveRecord::Migration
  def self.up

    create_table :active_dashboards do |t|
      t.column :dashboard_id, :integer, :null => false
      t.column :user_id, :integer, :null => true
      t.column :order_index, :integer, :null => true
    end
    add_index :active_dashboards, [:user_id], :name => 'active_dashboards_userid'
    add_index :active_dashboards, [:dashboard_id], :name => 'active_dashboards_dashboardid'

    create_table :dashboards do |t|
      t.column :user_id, :integer, :null => true
      t.column :name, :string, :null => true, :limit => 256
      t.column :description, :string, :null => true, :limit => 1000
      t.column :column_layout, :string, :null => true, :limit => 20
      t.column :shared, :boolean, :null => true
      t.timestamps
    end

    create_table :widgets do |t|
      t.column :dashboard_id, :integer, :null => false
      t.column :widget_key, :string, :null => false, :limit => 256
      t.column :name, :string, :null => true, :limit => 256
      t.column :description, :string, :null => true, :limit => 1000
      t.column :column_index, :integer, :null => true
      t.column :row_index, :integer, :null => true
      t.column :configured, :boolean, :null => true
      t.timestamps
    end
    add_index :widgets, [:dashboard_id], :name => 'widgets_dashboards'
    add_varchar_index :widgets, [:widget_key], :name => 'widgets_widgetkey'

    create_table :widget_properties do |t|
      t.column :widget_id, :integer, :null => false
      t.column :kee, :string, :null => true, :limit => 100
      t.column :text_value, :string, :null => true, :limit => 4000
    end
    add_index :widget_properties, [:widget_id], :name => 'widget_properties_widgets'
  end
end 

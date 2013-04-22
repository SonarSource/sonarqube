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
    if dialect=='mysql'
      # Index of varchar column is limited to 767 bytes on mysql (<= 255 UTF-8 characters)
      # See http://jira.codehaus.org/browse/SONAR-4137 and
      # http://dev.mysql.com/doc/refman/5.6/en/innodb-restrictions.html
      add_index :widgets, [:widget_key], :name => 'widgets_widgetkey', :length => 255
    else
      add_index :widgets, [:widget_key], :name => 'widgets_widgetkey'
    end

    create_table :widget_properties do |t|
      t.column :widget_id, :integer, :null => false
      t.column :kee, :string, :null => true, :limit => 100
      t.column :text_value, :string, :null => true, :limit => 4000
      t.column :value_type, :string, :null => true, :limit => 20
    end
    add_index :widget_properties, [:widget_id], :name => 'widget_properties_widgets'
  end
end 

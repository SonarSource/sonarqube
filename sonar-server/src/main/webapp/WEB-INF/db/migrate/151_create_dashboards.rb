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
    add_index :widgets, [:widget_key], :name => 'widgets_widgetkey'

    create_table :widget_properties do |t|
      t.column :widget_id, :integer, :null => false
      t.column :kee, :string, :null => true, :limit => 100
      t.column :text_value, :string, :null => true, :limit => 4000
      t.column :value_type, :string, :null => true, :limit => 20
    end
    add_index :widget_properties, [:widget_id], :name => 'widget_properties_widgets'

    add_default_dashboards()
  end

  private

  def self.create_dashboard
    dashboard=::Dashboard.new(:name => 'Dashboard', :shared => true, :description => 'Default dashboard', :column_layout => "50-50")
    dashboard.widgets.build(:widget_key => 'static_analysis', :name => 'Static analysis', :column_index => 1, :row_index => 1, :configured => true)
    dashboard.widgets.build(:widget_key => 'comments_duplications', :name => 'Comments duplications', :column_index => 1, :row_index => 2, :configured => true)
    dashboard.widgets.build(:widget_key => 'extended_analysis', :name => 'Extended analysis', :column_index => 1, :row_index => 3, :configured => true)
    dashboard.widgets.build(:widget_key => 'code_coverage', :name => 'Code coverage', :column_index => 1, :row_index => 4, :configured => true)
    dashboard.widgets.build(:widget_key => 'events', :name => 'Events', :column_index => 1, :row_index => 5, :configured => true)
    dashboard.widgets.build(:widget_key => 'description', :name => 'Description', :column_index => 1, :row_index => 6, :configured => true)
    dashboard.widgets.build(:widget_key => 'rules', :name => 'Rules', :column_index => 2, :row_index => 1, :configured => true)
    dashboard.widgets.build(:widget_key => 'alerts', :name => 'Alerts', :column_index => 2, :row_index => 2, :configured => true)
    dashboard.widgets.build(:widget_key => 'custom_measures', :name => 'Custom measures', :column_index => 2, :row_index => 3, :configured => true)
    dashboard.widgets.build(:widget_key => 'file-design', :name => 'File design', :column_index => 2, :row_index => 4, :configured => true)
    dashboard.widgets.build(:widget_key => 'package-design', :name => 'Package design', :column_index => 2, :row_index => 5, :configured => true)
    dashboard.widgets.build(:widget_key => 'ckjm', :name => 'CKJM', :column_index => 2, :row_index => 6, :configured => true)

    dashboard.save
    dashboard
  end

  def self.add_default_dashboards
    dashboard=create_dashboard()
    ActiveDashboard.create(:dashboard => dashboard, :user_id => nil, :order_index => 1)
  end
end 

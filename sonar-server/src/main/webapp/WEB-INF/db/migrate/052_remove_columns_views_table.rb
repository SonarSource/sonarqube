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
class RemoveColumnsViewsTable < ActiveRecord::Migration

  def self.up
    begin
      migrate_columns_selected
      migrate_column_default_sort
      migrate_treemap_enable
      
      drop_table "columns_views"
    rescue
    end
  end

  def self.down
  end

  class ColumnsView052 < ActiveRecord::Base
    set_table_name "columns_views"
  end


  private

  def self.migrate_columns_selected
    dashboard_configuration = Sonar::DashboardConfiguration.new(nil)

    columns_text = ""
    ColumnsView052.find(:all).each do |col|
      column = find_column_by_name_or_by_id(col.name, dashboard_configuration)
      columns_text << column.col_type + Sonar::DashboardConfiguration::COLUMN_SEPARATOR + column.id + Sonar::DashboardConfiguration::COLUMNS_SEPARATOR if column
    end
    create_property(Sonar::DashboardConfiguration::COLUMNS_SELECTED_KEY.to_s, columns_text)
  end

  def self.migrate_column_default_sort
    dashboard_configuration = Sonar::DashboardConfiguration.new(nil)

    default_sort_column = ColumnsView052.find(:first, :conditions => {:sort_default => true})
    column = find_column_by_name_or_by_id(default_sort_column.name, dashboard_configuration) if default_sort_column
    create_property(Sonar::DashboardConfiguration::COLUMNS_DEFAULT_SORT_KEY, column.id) if column
  end

  def self.migrate_treemap_enable
    treemap_enable = ColumnsView052.find(:first, :conditions => {:name => "Treemap"})
    is_enabled = treemap_enable ? "true" : "false"
    create_property(Sonar::DashboardConfiguration::TREEMAP_ENABLED_KEY, is_enabled)
  end


  def self.find_column_by_name_or_by_id(column_name_or_id, dashboard_configuration)
    dashboard_configuration.available_columns.each_pair do |domain, columns|
      columns.each do |column|
        if (column.name == column_name_or_id) or (column.id == column_name_or_id)
          return column
        end
      end
    end
    nil
  end

  def self.create_property(prop_key, prop_value)
    property = Property.new
    property.prop_key = prop_key
    property.prop_value = prop_value
    property.save!
  end


end

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
class Sonar::ComponentsConfiguration

  COLUMN_SEPARATOR = '.'
  COLUMNS_SEPARATOR = ';'

  COLUMNS_SELECTED_KEY = 'sonar.core.projectsdashboard.columns'
  COLUMNS_DEFAULT_SORT_KEY = 'sonar.core.projectsdashboard.defaultSortedColumn'

  def initialize
    @sorted_column_id=Property.value(COLUMNS_DEFAULT_SORT_KEY) || Sonar::ColumnsView::TYPE_PROJECT
    @text_columns=Property.value(COLUMNS_SELECTED_KEY) || default_text_columns
  end

  def selected_columns
    columns_from_text(@text_columns)
  end

  def homepage_metrics()
    metrics = selected_columns.select{|col| col.metric_column?}.collect{|col| Metric.by_name(col.id)}
    metrics<<Metric.by_name(Metric::ALERT_STATUS)
    metrics.uniq.compact
  end

  def addeable_columns
    addeable_columns = available_columns
    addeable_columns.each_key { |domain|
      domain_addeable_columns = addeable_columns[domain]
      domain_addeable_columns.delete_if { |columns_view|
        deleteable = false
        selected_columns.each do |selected_column|
          deleteable = true if columns_view.name == selected_column.name
        end
        deleteable
      }
    }
    addeable_columns
  end
  
  def find_selected_column(column_id)
    selected_columns.detect {|column| column.id == column_id}
  end

  def add_column(column)
    columns = selected_columns + [column]
    Property.set(COLUMNS_SELECTED_KEY, columns_to_text(columns))
  end

  def remove_column(column)
    columns = selected_columns.reject {|col| col.id == column.id}
    Property.set(COLUMNS_SELECTED_KEY, columns_to_text(columns))
  end

  def move_column(column, direction)
    position = column.position
    columns = selected_columns.reject {|col| col.id == column.id}
    new_position = (direction == "left") ? position - 1 : position + 1
    columns = columns.insert(new_position, column)
    Property.set(COLUMNS_SELECTED_KEY, columns_to_text(columns))
  end

  def find_available_column(column_id)
    available_columns.each_pair do |domain, columns|
      columns.each do |column|
        if column.id == column_id
          return column
        end
      end
    end
    nil
  end

  def sorted_column_id
    @sorted_column_id
  end
  
  def sorted_by_project_name?
    @sorted_column_id==Sonar::ColumnsView::TYPE_PROJECT
  end

  def set_column_sort_default(column_id)
    Property.set(COLUMNS_DEFAULT_SORT_KEY, column_id)
  end


  @@available_columns = nil

  DEFAULT_DOMAIN='General'

  def available_columns
    if @@available_columns.nil?
      @@available_columns = {}

      @@available_columns[DEFAULT_DOMAIN] = []
      col = Sonar::ColumnsView.new
      col.name = "Links"
      col.id = 'links'
      col.col_type = Sonar::ColumnsView::TYPE_LINKS
      @@available_columns[DEFAULT_DOMAIN] << col

      col = Sonar::ColumnsView.new
      col.name = "Build time"
      col.id = 'build_time'
      col.col_type = Sonar::ColumnsView::TYPE_BUILD_TIME
      @@available_columns[DEFAULT_DOMAIN] << col

      col = Sonar::ColumnsView.new
      col.name = "Language"
      col.id = 'language'
      col.col_type = Sonar::ColumnsView::TYPE_LANGUAGE
      @@available_columns[DEFAULT_DOMAIN] << col

      col = Sonar::ColumnsView.new
      col.name = "Version"
      col.id = 'version'
      col.col_type = Sonar::ColumnsView::TYPE_VERSION
      @@available_columns[DEFAULT_DOMAIN] << col
      
      Metric.all.select {|m| m.display?}.each do |metric|
        col = Sonar::ColumnsView.new
        col.name = metric.short_name
        col.id = metric.name
        col.col_type = Sonar::ColumnsView::TYPE_METRIC

        if col.name
          if metric.domain
            @@available_columns[metric.domain] ||= []
            @@available_columns[metric.domain] << col
          end
        end
      end

      @@available_columns.each_value {|columns|
        columns.sort! { |x, y|
          x.name = "" if x.name.nil?
          y.name = "" if y.name.nil?
          x.name <=> y.name if x.name && y.name
        }
      }

    end
    # must return a copy of the hash !
    available_columns_clone = {}
    @@available_columns.each_pair {|domain, columns|
      available_columns_clone[domain] = columns.clone
    }
    available_columns_clone
  end


  protected

  @@default_columns=nil
  
  def default_text_columns
    unless @@default_columns
      @@default_columns = ""
      @@default_columns << Sonar::ColumnsView::TYPE_METRIC + COLUMN_SEPARATOR + Metric::VIOLATIONS_DENSITY + COLUMNS_SEPARATOR
      @@default_columns << Sonar::ColumnsView::TYPE_METRIC + COLUMN_SEPARATOR + Metric::COVERAGE + COLUMNS_SEPARATOR
      @@default_columns << Sonar::ColumnsView::TYPE_BUILD_TIME + COLUMN_SEPARATOR + 'build_time' + COLUMNS_SEPARATOR
      @@default_columns << Sonar::ColumnsView::TYPE_LINKS + COLUMN_SEPARATOR + "links"
    end
    @@default_columns
  end

  def columns_from_text(text)
    columns = []
    text.split(COLUMNS_SEPARATOR).each_with_index do |column_text, position|
      column = column_from_text(column_text)
      if column
        column.position = position
        column.sort_default = (column.id==@sorted_column_id)
        columns << column
      end
    end
    columns
  end

  def column_from_text(text)
    column_split = text.split(COLUMN_SEPARATOR)
    column = Sonar::ColumnsView.new
    column.id = column_split[1]
    column.col_type = column_split[0]
    available_col=find_available_column(column.id)
    if available_col
      column.name = available_col.name
      column
    else 
      nil
    end
  end

  def columns_to_text(columns)
    text = ""
    columns.each do |column|
      text << column_to_text(column) + COLUMNS_SEPARATOR
    end
    text
  end

  def column_to_text(column)
    text = column.col_type + COLUMN_SEPARATOR + column.id
  end

end

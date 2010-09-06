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
class Sonar::ColumnsView

  TYPE_PROJECT = 'PROJECT'
  TYPE_BUILD_TIME = 'BUILD_TIME'
  TYPE_LINKS = 'LINKS'
  TYPE_METRIC = 'METRIC'
  TYPE_RULES_CATEGORY = 'RULES_CATEG'
  TYPE_TREE_MAP = 'TREEMAP'
  TYPE_LANGUAGE = 'LANGUAGE'
  TYPE_VERSION = 'VERSION'


  attr_accessor :id, :name, :col_type, :position, :sort_default

  def project_column?
    @col_type == TYPE_PROJECT
  end
  
  def links_column?
    @col_type == TYPE_LINKS
  end

  def build_time_column?
    @col_type == TYPE_BUILD_TIME
  end

  def metric_column?
    @col_type == TYPE_METRIC || @col_type == TYPE_RULES_CATEGORY
  end

  def language_column?
    @col_type == TYPE_LANGUAGE
  end

  def version_column?
    @col_type == TYPE_VERSION
  end

  def text_column?
    project_column? || links_column? || (get_metric && get_metric.val_type==Metric::VALUE_TYPE_STRING)
  end

  def sort_default?
    sort_default
  end

  def get_metric
    @metric ||= Metric.by_name(id)
  end

  def sortable?
    !links_column?
  end
  
  def get_table_header_css
    css_class = 'right '
    css_class = css_class + "nosort" if !sortable?
    css_class = css_class + "text" if sortable? && text_column?
    css_class = css_class + "number" if sortable? && !text_column?
    css_class = get_default_css_sort + css_class
    css_class
  end

  def get_table_header_css_no_sort
    css_class = 'right '
    if sort_default?
      if build_time_column?
        css_class = css_class + "sortfirstdesc " 
      elsif !metric_column? || get_metric.direction == 1
        css_class = css_class + "sortfirstasc "
      elsif get_metric.direction == -1
        css_class = css_class + "sortfirstdesc "
      end
    end
    css_class
  end

  def get_default_css_sort
    return "" if !sort_default?
    return "sortfirstdesc " if build_time_column?
    return "sortfirstasc " if !metric_column? || get_metric.direction == 1
    return "sortfirstdesc " if get_metric.direction == -1
    ""
  end

end

#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
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
module FiltersHelper

  def column_title(column, filter)
    if column.sortable?
      html=link_to h(column.display_name), url_for(:overwrite_params => {:asc => (!(column.ascending?)).to_s, :sort => column.id})
    else
      html=h(column.display_name)
    end
    if column.variation
      html="<img src='#{ApplicationController.root_context}/images/trend-up.png'></img> #{html}"
    end

    if filter.sorted_column==column
      html << (column.ascending? ? image_tag("asc12.png") : image_tag("desc12.png"))
    end
    html
  end

  def column_align(column)
    (column.on_name? || column.on_key?) ? 'left' : 'right' 
  end

  def treemap_metrics(filter)
    metrics=filter.measure_columns.map{|col| col.metric}
    size_metric=(metrics.size>=1 ? metrics[0] : Metric.by_key('ncloc'))
    color_metric=nil
    if metrics.size>=2
      color_metric=metrics[1]
    end
    if color_metric.nil? || !color_metric.treemap_color?
      color_metric=Metric.by_key('violations_density')
    end
    [size_metric, color_metric]
  end

  def period_names
    p1=Property.value('sonar.timemachine.period1', nil, 'previous_analysis')
    p2=Property.value('sonar.timemachine.period2', nil, '5')
    p3=Property.value('sonar.timemachine.period3', nil, '30')
    [period_name(p1), period_name(p2), period_name(p3)]
  end

  private

  def period_name(property)
    if property=='previous_analysis'
      message('delta_since_previous_analysis')
    elsif property =~ /^[\d]+(\.[\d]+){0,1}$/
      # is integer
      message('delta_over_x_days', :params => property)
    elsif property =~ /\d{4}-\d{2}-\d{2}/
      message('delta_since', :params => property)
    elsif !property.blank?
      message('delta_since_version', :params => property)
    else
      nil
    end
  end
end
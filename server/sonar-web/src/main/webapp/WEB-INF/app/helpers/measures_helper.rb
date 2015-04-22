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
module MeasuresHelper

  def list_column_html(filter, column, widget_id)
    if column.sort?
      html = link_to_function(h(column.title_label), "refreshList#{widget_id}('#{escape_javascript column.key}',#{!filter.sort_asc?}, '#{filter.criteria[:page]||1}')", :title => h(column.tooltip))
    else
      html=h(column.title_label)
    end
    if column.period
      html += "<br><span class='note'>#{Api::Utils.period_abbreviation(column.period)}</small>"
    end
    if column.sort? && filter.sort_key==column.key
      html << (filter.sort_asc? ? ' <i class="icon-sort-asc"></i>' : ' <i class="icon-sort-desc"></i>')
    end
    "<th class='#{column.align} #{column.title_css}'>#{html}</th>"
  end

  def list_cell_html(column, row)
    if column.metric
      measure = row.measure(column.metric)
      if column.period
        if measure && measure.metric.on_new_code?
          format_new_metric_measure(measure, column.period)
        else
          format_variation(measure, :index => column.period, :style => 'light')
        end
      else
        format_measure(measure) + ' '
      end

    elsif column.key=='name'
      "#{qualifier_icon(row.snapshot)} #{link_to(h(row.snapshot.resource.name(true)), {:controller => 'dashboard', :id => row.snapshot.resource_id}, {:title => h(row.snapshot.resource.key)})}"
    elsif column.key=='short_name'
      "#{qualifier_icon(row.snapshot)} #{link_to(h(row.snapshot.resource.name(false)), {:controller => 'dashboard', :id => row.snapshot.resource_id}, {:title => h(row.snapshot.resource.key)})}"
    elsif column.key=='date'
      human_short_date(row.snapshot.created_at)
    elsif column.key=='project_creation_date'
      human_short_date(row.snapshot.resource.created_at) if row.snapshot.resource.created_at
    elsif column.key=='key'
      "<span class='small'>#{row.snapshot.resource.kee}</span>"
    elsif column.key=='description'
      h row.snapshot.resource.description
    elsif column.key=='version'
      h row.snapshot.version
    elsif column.key=='language'
      Api::Utils.language_name(row.snapshot.resource.language)
    elsif column.key=='links' && row.links
      html = ''
      row.links.select { |link| link.href.start_with?('http') }.each do |link|
        html += "<a target='_blank' href='#{link.href}' class='icon-#{link.link_type}'></a> " unless link.custom?
      end
      html
    end
  end

  def measure_filter_star(filter, is_favourite)
    if is_favourite
      style='icon-favorite'
      title=message('click_to_remove_from_favorites')
    else
      style='icon-not-favorite'
      title=message('click_to_add_to_favorites')
    end

    "<a href='#' id='star-#{filter.name.parameterize}' class='measure-filter-star #{style}' filter-id='#{filter.id}' title='#{title}'></a>"
  end

  def period_labels
    [Api::Utils.period_label(1), Api::Utils.period_label(2), Api::Utils.period_label(3)]
  end

  def more_criteria_options(filter)
    more_criteria_options = [['', '']]
    more_criteria_options << [message('measure_filter.criteria.metric'), 'metric']
    more_criteria_options << [message('measure_filter.criteria.age'), 'age'] unless filter.criteria('ageMinDays') || filter.criteria('ageMaxDays')
    more_criteria_options << [message('measure_filter.criteria.date'), 'date'] unless filter.criteria('fromDate') || filter.criteria('toDate')
    more_criteria_options << [message('measure_filter.criteria.only_favorites'), 'fav'] unless filter.criteria('onFavourites')
    more_criteria_options << [message('measure_filter.criteria.key'), 'key'] unless filter.criteria('keySearch')
    more_criteria_options << [message('measure_filter.criteria.language'), 'lang'] unless filter.criteria('languages')
    more_criteria_options << [message('measure_filter.criteria.name'), 'name'] unless filter.criteria('nameSearch')
    more_criteria_options << [message('measure_filter.criteria.components_of_project'), 'project'] unless filter.base_resource
    more_criteria_options << [message('measure_filter.criteria.alert'), 'alert'] unless filter.criteria('alertLevels')
    # SONAR-4508 sort criterias
    Api::Utils.insensitive_sort(more_criteria_options){|option| option[0]}
  end


  private

  #
  # This method is a inspired by ApplicationHelper#format_variation for measure where metrics key begin with 'new_'
  # It will display measure in color, without operand (+/-), and prefix with a %.
  #
  def format_new_metric_measure(metric_or_measure, index)
    if metric_or_measure.is_a?(ProjectMeasure)
      m = metric_or_measure
    elsif @snapshot
      m = @snapshot.measure(metric_or_measure)
    end
    html=nil
    if m
      val=variation_value(m, :index => index)
      if val
        formatted_val= m.format_numeric_value(val, :variation => false)
        css_class='var'
        if m.metric.qualitative?
          factor=m.metric.direction * val
          if factor>0
            # better
            css_class='varb'
          elsif factor<0
            # worst
            css_class='varw'
          end
        end

        html="<span class='#{css_class}'>#{formatted_val}</span>"
      end
    end
    html
  end

end

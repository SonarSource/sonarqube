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
module PropertiesHelper

  SCREEN_SETTINGS = 'SETTINGS'
  SCREEN_WIDGET = 'WIDGET'
  SCREEN_RULES = 'RULES'

  #
  # screen is SETTINGS, WIDGET or RULES
  # ==== Options
  # * <tt>:id</tt> - html id to use if the name should not be used
  # * <tt>:size</tt>
  # * <tt>:values</tt> - list of values for metric and single select list
  # * <tt>:default</tt> - default value
  # * <tt>:disabled</tt>
  # * <tt>:extra_values</tt>
  #
  def property_input_field(name, type, value, screen, options = {})

    html_options = {:id => options[:id] || name}
    html_options[:disabled] = 'disabled' if options[:disabled]

    case type

      when PropertyType::TYPE_STRING
        text_field_tag name, value, {:size => options[:size] || 50}.update(html_options)

      when PropertyType::TYPE_TEXT
        cols = options[:size] || nil
        html_class = cols.nil? ? ' width100' : ''
        text_area_tag name, value, {:class => html_class, :rows => '5', :cols => cols}.update(html_options)

      when PropertyType::TYPE_PASSWORD
        password_field_tag name, value, {:size => options[:size] || 50, :autocomplete => 'off'}.update(html_options)

      when PropertyType::TYPE_BOOLEAN
        select_options = "<option value='' #{ 'selected' if value.blank? }>#{ message('default') }</option>"
        select_options += "<option value='true' #{ 'selected' if value=='true' }>#{ message('true') }</option>"
        select_options += "<option value='false' #{ 'selected' if value=='false' }>#{ message('false') }</option>"
        select_tag name, select_options, html_options

      when PropertyType::TYPE_INTEGER
        size = options[:size] || 10
        text_field_tag name, value, {:size => size}.update(html_options)

      when PropertyType::TYPE_FLOAT
        size = options[:size] || 10
        text_field_tag name, value, {:size => size}.update(html_options)

      when PropertyType::TYPE_METRIC
        metric_select_tag name, metrics_filtered_by(options[:values]), {:html_id => options[:id], :selected_key => value, :allow_empty => true,
                                                                        :placeholder => !options[:default].blank? ?  message('default') : nil}

      when PropertyType::TYPE_REGULAR_EXPRESSION
        size = options[:size] || 50
        text_field_tag name, value, {:size => size}.update(html_options)

      when PropertyType::TYPE_FILTER
        user_filters = options_id(value, current_user.measure_filters)
        shared_filters = options_id(value, MeasureFilter.find(:all, :conditions => ['(user_id<>? or user_id is null) and shared=?', current_user.id, true]).sort_by(&:name))

        filters_combo = select_tag name, option_group('My Filters', user_filters) + option_group('Shared Filters', shared_filters), html_options
        filter_link = link_to message('widget.filter.edit'), {:controller => 'measures', :action => 'manage'}, :class => 'link-action'

        "#{filters_combo} #{filter_link}"

      when PropertyType::TYPE_ISSUE_FILTER
        user_filters = options_id(value, Internal.issues.findIssueFiltersForCurrentUser())
        shared_filters = options_id(value, Internal.issues.findSharedFiltersForCurrentUser())

        #user_filters = options_id(value, current_user.measure_filters)
        #shared_filters = options_id(value, MeasureFilter.find(:all, :conditions => ['(user_id<>? or user_id is null) and shared=?', current_user.id, true]).sort_by(&:name))

        filters_combo = select_tag name, option_group('My Filters', user_filters) + option_group('Shared Filters', shared_filters), html_options
        filter_link = link_to message('widget.filter.edit'), {:controller => 'issues', :action => 'manage'}, :class => 'link-action'

        "#{filters_combo} #{filter_link}"

      when PropertyType::TYPE_SINGLE_SELECT_LIST
        default_value = options[:default].blank? ? '' : message('default')
        select_options = "<option value=''>#{ default_value }</option>"
        options[:values].each do |option|
          message = screen == SCREEN_WIDGET ? option_name_with_key(name, nil, option, 'widget.'+ options[:extra_values][:widget_key]) :
              option_name(options[:extra_values][:property], options[:extra_values][:field], option)
          select_options += "<option value='#{ html_escape option }' #{ 'selected' if value && value==option }>#{ message }</option>"
        end
        select_tag name, select_options, html_options

      when PropertyType::TYPE_USER_LOGIN
        user = User.find_active_by_login(value)
        user_select_tag name, {:size => options[:size] || 50, :selected_user => user, :allow_empty => true, :include_choices => [ ['', message('none')] ] }.update(html_options)

      else
        hidden_field_tag id, html_options
    end
  end

  def options_id(value, values)
    values.collect { |f| "<option value='#{f.id}'" + (value.to_s == f.id.to_s ? " selected='selected'" : "") + ">#{h(f.name)}</option>" }.to_s
  end

  def options_key(value, values)
    values.collect { |f| "<option value='#{h(f.key)}'" + (value.to_s == f.key ? " selected='selected'" : "") + ">#{h(f.name)}</option>" }.to_s
  end

  def option_group(name, options)
    options.empty? ? '' : "<optgroup label=\"#{h(name)}\">" + options + "</optgroup>"
  end

  def metrics_filtered_by(options)
    Metric.all.select(&:display?).sort_by(&:short_name).select do |metric|
      options.blank? || options.all? { |option| metric_matches(metric, option) }
    end
  end

  def metric_matches(metric, option)
    if /key:(.*)/.match(option)
      Regexp.new(Regexp.last_match(1).strip).match(metric.key)
    elsif /domain:(.*)/.match(option)
      Regexp.new(Regexp.last_match(1)).match(metric.domain)
    elsif /type:(.*)/.match(option)
      Regexp.last_match(1).split(',').any? { |type| (type == metric.value_type) || ((type == 'NUMERIC') && metric.numeric?) }
    else
      false
    end
  end

  def option_name_with_key(property_key, field_key, option, key_prefix = '')
    prefix = ''
    prefix = key_prefix + "." if !key_prefix.blank?
    if field_key
      # Old key used for retro-compatibility
      message = message(prefix +"option.#{property_key}.#{field_key}.#{option}.name", :default => '')
      message = message(prefix +"property.#{property_key}.#{field_key}.option.#{option}.name", :default => option) unless message != ''
      message
    else
      # Old key used for retro-compatibility
      message = message(prefix +"option.#{property_key}.#{option}.name", :default => '')
      message = message(prefix +"property.#{property_key}.option.#{option}.name", :default => option) unless message != ''
      message
    end
  end

end

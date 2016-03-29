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
module ApplicationHelper

  # hack for firefox. The 'alt' parameter on images does not work. Firefox uses 'title' instead.
  # http://snippets.dzone.com/posts/show/2917
  def image_tag(location, options={})
    options[:title] ||= options[:alt]
    super(location, options)
  end

  # Since 3.6
  # java.util.Date is supported
  #
  # == Options
  # * :format - See Ruby on Rails localization options
  # * :time_if_today - true to only display the time when the date is today.
  #
  def format_datetime(object, options={})
    return nil unless object
    if object.is_a?(Java::JavaUtil::Date)
      dt = Api::Utils.java_to_ruby_datetime(object)
    else
      dt = object
    end
    if options[:time_if_today] && (Date.today - date.to_date == 0)
      dt.strftime('%H:%M')
    else
      l(dt, options)
    end
  end

  # Since 3.6
  # java.util.Date is supported
  #
  # == Options
  # * :format - values are :short, :default, :long. See Ruby on Rails localization for more details.
  # * :time_if_today - true to only display the time when the date is today.
  #
  def format_date(object, options={})
    return nil unless object

    dt = object
    date = object
    if object.is_a?(Java::JavaUtil::Date)
      dt = Api::Utils.java_to_ruby_datetime(object)
      date = dt.to_date
    elsif object.is_a?(DateTime)
      dt = object
      date = object.to_date
    end
    if options[:time_if_today] && (Date.today - date.to_date == 0)
      dt.strftime('%H:%M')
    else
      l(date, options)
    end
  end

  # Override date_helper methods to be consistent with Java age format
  # java.util.Date and Time (ruby) are supported
  #
  def distance_of_time_in_words_to_now(date)
    Internal.i18n.ageFromNow(date)
  end

  def sonar_version
    Java::OrgSonarServerPlatform::Platform.getServer().getVersion()
  end

  def property_value_by_key(key)
    property = Property.by_key(key)
    property.value if property
  end

  # shortcut for the method is_admin?() without parameters. Result is kept in cache.
  def administrator?
    @is_administrator ||=
        begin
          is_admin?
        end
  end

  # shortcut for the method has_role?(:profileadmin). Result is kept in cache.
  def profiles_administrator?
    @is_profileadmin ||=
        begin
          has_role?(:profileadmin)
        end
  end

  def qualifier_icon(object)
    qualifier=(object.respond_to?('qualifier') ? object.qualifier : object.to_s)
    if qualifier
      "<i class=\"icon-qualifier-#{qualifier.downcase}\"></i>"
    else
      image_tag 'e16.gif'
    end
  end

  def human_short_date(date)
    if Date.today - date.to_date == 0
      date.strftime('%H:%M')
    else
      l(date.to_date)
    end
  end

  # i18n
  def message(key, options={})
    Api::Utils.message(key, options)
  end

  # deprecated since 2.5. Use trend_icon() instead
  def tendency_icon(metric_or_measure, small=true, no_tendency_img=true)
    trend_icon(nil, {:empty => !no_tendency_img})
  end

  def boolean_icon(boolean_value, options={})
    if boolean_value
      "<i class='icon-check'></i>"
    elsif options[:display_false]
      image_tag('cross.png', options)
    else
      nil
    end
  end

  def configuring?
    params['configuring']=='true'
  end

  def configuration(key, default = nil)
    prop_value = Java::OrgSonarServerUi::JRubyFacade.getInstance().getConfigurationValue(key)
    prop_value.nil? ? default : prop_value
  end

  def metric(key)
    Metric.by_key(key)
  end

  # URL to static resource.
  #
  # === Optional parameters
  #
  # * <tt>:plugin</tt> - key of plugin, from where this resource should be loaded.
  #
  # === Examples
  #
  #   url_for_static(:path => 'images/sonarqube-24x100.png')
  #   url_for_static(:plugin => 'myplugin', :path => 'image.png')
  def url_for_static(options={})
    if options[:plugin]
      "#{ApplicationController.root_context}/static/#{options[:plugin]}/#{options[:path]}"
    else
      "#{ApplicationController.root_context}/#{options[:path]}"
    end
  end

  # URL to measures drilldown page for a given metric.
  #
  # === Optional parameters
  #
  # * <tt>:resource</tt> - id or key of the selected resource
  # * <tt>:highlight</tt> - key of the metric to highlight, different than the metric to drilldown.
  # * <tt>:period</tt> - period index
  #
  # === Examples
  #
  #   url_for_drilldown('ncloc')
  #   url_for_drilldown('ncloc', {:resource => 'org.apache.struts:struts-parent', :highlight => 'lines'})
  #   url_for_drilldown('ncloc', {:resource => 'org.apache.struts:struts-parent'})
  #
  def url_for_drilldown(metric_or_measure, options={})
    if options[:resource].nil? && !@resource
      return ''
    end

    if metric_or_measure.is_a? ProjectMeasure
      metric_key = metric_or_measure.metric.key
    elsif metric_or_measure.is_a? Metric
      metric_key = metric_or_measure.key
    else
      metric_key = metric_or_measure
    end

    issues_metrics = get_issue_metrics

    if issues_metrics.include? metric_key
      resource = options[:resource]||@resource.key
      url = url_for({:controller => 'component_issues', :action => 'index'}) + '?id=' + url_encode(resource) + '#'
      if options[:period] && @snapshot
        snapshot_datetime = @snapshot.period_datetime(options[:period])
        if snapshot_datetime
          date = snapshot_datetime.strftime('%FT%T%z')
          url += "createdAfter=#{date}|"
        end
      end
      case metric_key
        when 'blocker_violations', 'new_blocker_violations'
          url += 'resolved=false|severities=BLOCKER'
        when 'critical_violations', 'new_critical_violations'
          url += 'resolved=false|severities=CRITICAL'
        when 'major_violations', 'new_major_violations'
          url += 'resolved=false|severities=MAJOR'
        when 'minor_violations', 'new_minor_violations'
          url += 'resolved=false|severities=MINOR'
        when 'info_violations', 'new_info_violations'
          url += 'resolved=false|severities=INFO'
        when 'open_issues'
          url += 'resolved=false|statuses=OPEN'
        when 'reopened_issues'
          url += 'resolved=false|statuses=REOPENED'
        when 'confirmed_issues'
          url += 'resolved=false|statuses=CONFIRMED'
        when 'false_positive_issues'
          url += 'resolutions=FALSE-POSITIVE'
        else
          url += 'resolved=false'
      end
    else
      url = url_for(options.merge({:controller => 'drilldown', :action => 'measures', :metric => metric_key, :id => options[:resource]||@resource.id}))
    end

    url
  end

  #
  # Display a measure
  #
  # === Optional parameters
  # * <tt>:skip_span_id</tt> - skip the generation of the html attribute 'id'. Default is false
  # * <tt>:url</tt> - add an url on the measure.
  # * <tt>:prefix</tt> - add a prefix. Default is ''.
  # * <tt>:suffix</tt> - add a suffix. Default is ''.
  # * <tt>:period</tt> - period index, from 1 to 5. Optional. Default is nil.
  # * <tt>:default</tt> - text to return if metric or measure not found. Default is blank string.
  #
  # === Examples
  #
  #   format_measure('ncloc')
  #   format_measure('ncloc', {:suffix => '', :url => url_for_drilldown('ncloc'), :default => '-'})
  #
  def format_measure(metric_or_measure, options={})
    html=''
    m=nil
    if metric_or_measure.is_a? ProjectMeasure
      m = metric_or_measure
    elsif @snapshot
      m = @snapshot.measure(metric_or_measure)
    end

    if m.nil?
      return options[:default] || ''
    end

    if m && m.metric
      link_rel=''
      show_link= !options[:url].blank?

      if options[:period]
        html=m.format_numeric_value(m.variation(options[:period].to_i))
      elsif m.metric.val_type==Metric::VALUE_TYPE_LEVEL
        html="<i class=\"icon-alert-#{m.alert_status.downcase}\"></i>" unless m.alert_status.blank?
      else
        html=m.formatted_value
      end

      alert_class=''
      style = ''
      if !(m.alert_status.blank?)
        alert_class="class='alert_#{m.alert_status}'" unless m.metric.val_type==Metric::VALUE_TYPE_LEVEL
        link_rel=h(m.alert_text)
      elsif m.metric.val_type==Metric::VALUE_TYPE_RATING && m.color
        alert_class="class='rating rating-" + m.formatted_value + "'"
      end

      span_id=''
      unless options[:skip_span_id]
        span_id="id='m_#{m.key}'"
      end
      html="<span #{span_id} #{alert_class} #{style}>#{html}</span>"
      if options[:prefix]
        html="#{options[:prefix]}#{html}"
      end
      if options[:suffix]
        html="#{html}#{options[:suffix]}"
      end

      if show_link
        if options[:url].blank?
          url='#'
          link_class='nolink'
        else
          url=options[:url]
          link_class=''
        end
        # Do not put url between simple quotes to prevent problem if url contain simple quote
        html='<a href="'+ url +'"'+ " class='widget-link #{link_class}' rel='#{link_rel}' title='#{link_rel}'>#{html}</a>"
      end
    end
    html
  end


  #
  #
  # link to the current page with the given resource. If file, then open a popup to display resource viewers.
  #
  #
  def link_to_resource(resource, name=nil, options={})
    period_index=options[:period]
    period_index=nil if period_index && period_index<=0
    if resource.display_dashboard?
      if options[:dashboard]
        root = "#{ApplicationController.root_context}/dashboard/index?"
      else
        # stay on the same page (for example components)
        root = "#{ApplicationController.root_context}/#{u params[:controller]}/#{u params[:action]}?"
      end
      path = ''
      query = request.query_parameters
      query[:id] = resource.id
      query[:metric] = options[:metric] if options[:metric]
      query[:period] = period_index if period_index
      query.each do |key, value|
        path += '&' unless path.empty?
        path += "#{u key}=#{u value}"
      end
      "<a class='#{options[:class]}' title='#{options[:title]}' href='#{root + path}'>#{name || resource.name}</a>"
    else
      url = "#{ApplicationController.root_context}/dashboard/index?id=#{u resource.key}"
      url += "&period=#{u period_index}" if period_index
      url += "&metric=#{u options[:metric]}" if options[:metric]
      "<a class='#{options[:class]}' title='#{options[:title]}' " +
          "onclick='window.open(this.href,\"resource-#{resource.key.parameterize}\",\"\");return false;' " +
          "href='#{url}'>#{name || resource.name}</a>"
    end
  end


  #
  #
  # Piechart for a distribution string or measure (foo=1;bar=2)
  #
  #
  def piechart(distribution, options={})
    chart = ""
    data=nil
    if distribution
      data=(distribution.kind_of? ProjectMeasure) ? distribution.data : distribution
    end

    if data && data.size > 0
      labels = []
      values = []
      skipZeros = options[:skipZeros].nil? ? true : options[:skipZeros]
      options[:skipZeros] = nil
      data.split(';').each do |pair|
        splitted = pair.split('=')
        value = splitted[1]
        next if skipZeros && value.to_i == 0
        labels << splitted[0]
        values << value
      end
      if labels.size > 0
        options[:alt] ||= ""
        chart = gchart("chs=#{options[:size] || '250x90'}&chd=t:#{values.join(',')}&cht=p&chl=#{labels.join('|')}", options)
      end
    end
    chart
  end

  #
  #
  # Draw a HTML/CSS bar
  #
  # === Optional parameters
  # * width: container width in pixels. Default is 150.
  # * percent: integer between -100 and 100. Size of the bar inside the container. Default is 100. Bar is aligned to right if the value is negative.
  # * color: the bar HTML color. Default value is '#777'
  # * tooltip: tooltip to display on the bar
  #
  def barchart(options)
    percent = (options[:percent] || 100).to_i
    width=(options[:width] || 150).to_i
    if options[:positive_color] && percent>0
      color = options[:positive_color]
    elsif options[:negative_color] && percent<0
      color = options[:negative_color]
    else
      color = options[:color]||'#236a97'
    end

    align=(percent<0 ? 'float: right;' : nil)
    "<div class='barchart' style='width: #{width}px' title='#{options[:tooltip]}'><div style='width: #{percent.abs}%;background-color:#{color};#{align}'></div></div>"
  end

  def chart(parameters, options={})
    image_tag("#{ApplicationController.root_context}/chart?#{parameters}", options)
  end

  def link_to_favourite(resource, deprecated_options=nil)
    return '' unless (logged_in?)
    return '' if resource.nil?
    resource_id=(resource.is_a?(Fixnum) ? resource : resource.permanent_id)

    if current_user.favourite?(resource_id)
      css='icon-favorite'
      title=message('click_to_remove_from_favorites')
    else
      css='icon-not-favorite'
      title=message('click_to_add_to_favorites')
    end
    link_to_function '', "toggleFav(#{resource_id}, this)", :class => css, :title => title
  end

  #
  # Unsupported since version 5.2
  #
  def trend_icon(metric_or_measure, options={})
    return options[:empty] ? "<i class=\"icon-trend-0\"></i>" : nil
  end

  #
  #
  # Numeric value of variation
  #
  # === Optional parameters
  # * index: integer between 1 and 5. By default the index is defined by the dashboard variation select-box
  #
  # === Examples
  # variation_value('ncloc')
  # variation_value(measure('ncloc'))
  # variation_value('ncloc', :period => 3)
  #
  def variation_value(metric_or_measure, options={})
    if metric_or_measure.is_a?(ProjectMeasure)
      m = metric_or_measure
    elsif @snapshot
      m = @snapshot.measure(metric_or_measure)
    end

    index=options[:period]||options[:index]
    if index.nil? && defined?(@dashboard_configuration) && @dashboard_configuration.selected_period?
      index = @dashboard_configuration.period_index
    end

    if m
      m.variation(index)||options[:default]
    else
      options[:default]
    end
  end

  #
  #
  # Format variation value
  #
  # === Optional parameters
  # * period: integer between 1 and 5. By default the index is defined by the dashboard variation select-box
  # * style: light|normal|none. Default is normal (parenthesis + bold)
  #
  # === Examples
  # format_variation('ncloc')
  # format_variation(measure('ncloc'), :period => 3, :color => true)
  #
  def format_variation(metric_or_measure, options={})
    if metric_or_measure.is_a?(ProjectMeasure)
      m = metric_or_measure
    elsif @snapshot
      m = @snapshot.measure(metric_or_measure)
    end
    html=nil
    if m
      val=variation_value(m, options)
      if val
        if options[:style]=='none'
          return m.format_numeric_value(val)
        end

        formatted_val= m.format_numeric_value(val, :variation => true)
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

        if options[:style]!='light'
          formatted_val=(val>=0 ? "+" : "") + formatted_val
          formatted_val="(#{formatted_val})"
        else
          # if zero, then we do not put a '+' before in the 'light' case
          formatted_val=(val>0 ? "+" : "") + formatted_val
        end
        html="<span class='#{css_class}'>#{formatted_val}</span>"
      end
    end
    html = options[:default].to_s if html.nil? && options[:default]
    html
  end

  #
  # Creates a pagination section for the given array (items_array) if its size exceeds the pagination size (default: 20).
  # Upon completion of this method, the HTML is returned and the given array contains only the selected elements.
  #
  # In any case, the HTML that is returned contains the message 'x results', where x is the total number of elements
  # in the items_array object.
  #
  # === Optional parameters
  # * page_size: the number of elements to display at the same time (= the pagination size)
  #
  def paginate(items_array, options={})
    html = items_array.size.to_s + " " + message('results').downcase

    page_size = options[:page_size] || 20
    if items_array.size > page_size
      # computes the pagination elements
      page_id = (params[:page_id] ? params[:page_id].to_i : 1)
      page_count = items_array.size / page_size
      page_count += 1 if (items_array.size % page_size > 0)
      from = (page_id-1) * page_size
      to = (page_id*page_size)-1
      to = items_array.size-1 if to >= items_array.size

      # render the pagination links
      html += " | "
      html += link_to_if page_id>1, message('paging_previous'), {:overwrite_params => {:page_id => page_id-1}}
      html += " "
      for index in 1..page_count
        html += link_to_unless index==page_id, index.to_s, {:overwrite_params => {:page_id => index}}
        html += " "
      end
      html += link_to_if page_id<page_count, message('paging_next'), {:overwrite_params => {:page_id => 1+page_id}}

      # and adapt the items_array object according to the pagination
      items_to_keep = items_array[from..to]
      items_array.clear
      items_to_keep.each { |i| items_array << i }
    end

    html
  end

  def select2_tag(name, ws_url, options={})
    width=options[:width]||'250px'
    html_id=options[:html_id]||sanitize_to_id(name).gsub('.', '_')
    html_class=options[:html_class]||''
    min_length=options[:min_length]

    js_options={
        'minimumInputLength' => min_length,
        'allowClear' => options[:allow_clear]||false,
        'formatNoMatches' => "function(term){return '#{escape_javascript message('select2.noMatches')}'}",
        'formatSearching' => "function(){return '#{escape_javascript message('select2.searching')}'}",
        'formatInputTooShort' => "function(term, minLength){return '#{escape_javascript message('select2.tooShort', :params => [min_length])}'}"
    }
    js_options['placeholder']= "'#{options[:placeholder]}'" if options.has_key?(:placeholder)
    js_options['width']= "'#{width}'" if width

    ajax_options={
      'quietMillis' => 300,
      'url' => "'#{ws_url}'",
      'data' => 'function (term, page) {return {s:term, p:page}}',
      'results' => 'function (data, page) {return {more: data.more, results: data.results}}'
    }
    ajax_options.merge!(options[:select2_ajax_options]) if options[:select2_ajax_options]

    choices = options[:include_choices]
    if choices && !choices.empty?
      js_options['minimumInputLength']=0
      js_options['query']="
        function(query) {
          if (query.term.length == 0) {
            query.callback({results: [{#{ choices.map { |id, text| "id:'#{id}',text:'#{text}'" }.join('}, {')}}]});
          } else if (query.term.length >= #{min_length}) {
            var dataFormatter = #{ajax_options['data']};
            var resultFormatter = #{ajax_options['results']};
            $j.ajax('#{ws_url}', {
                data: dataFormatter(query.term)
              }).done(function(data) {
                query.callback(resultFormatter(data));
            });
          }
      }"
    else
      js_options['ajax']='{' + ajax_options.map { |k, v| "#{k}:#{v}" }.join(',') + '}'
    end

    js_options.merge!(options[:select2_options]) if options[:select2_options]

    html = "<input type='hidden' id='#{html_id}' class='#{html_class}' name='#{name}'/>"
    js = "$j('##{html_id}').select2({#{js_options.map { |k, v| "#{k}:#{v}" }.join(',')}});"

    selected_id=options[:selected_id]
    selected_text=options[:selected_text]
    if selected_id && selected_text
      js += "$j('##{html_id}').select2('data', {id: #{selected_id}, text: '#{escape_javascript(selected_text)}'});"
    end
    if options[:open]
      js += "$j('##{html_id}').select2('open');"
    end

    "#{html}<script>$j(document).ready(function(){#{js}});</script>"
  end


  #
  # Creates an enhanced dropdown selection box of resources. Values are loaded on the fly via Ajax requests.
  # ==== Options
  # * <tt>:width</tt> - The width suffixed with unit, for example '300px' or '100%'. Default is '250px'
  # * <tt>:html_id</tt> - The id of the HTML element. Default is the name.
  # * <tt>:html_class</tt> - The class of the HTML element. Default is empty.
  # * <tt>:qualifiers</tt> - Array of resource qualifiers to filter.
  # * <tt>:resource_type_property</tt> -Filter on resource types on which the property is enabled, for example 'supportsGlobalDashboards'.
  # * <tt>:selected_resource</tt> - the resource that is selected by default.
  # * <tt>:placeholder</tt> - the label to display when nothing is selected
  # * <tt>:allow_clear</tt> - true if resource can be de-selected. Default is false.
  # * <tt>:open</tt> - true if the select-box must be open. Default is false.
  # * <tt>:select2_options</tt> - hash of select2 options
  #
  def resource_select_tag(name, options={})
    # see limitation in /api/resources/search
    options[:min_length]=2

    ws_url="#{ApplicationController::root_context}/api/resources/search?f=s2&"
    if options[:qualifiers]
      ws_url+="q=#{options[:qualifiers].join(',')}"
    elsif options[:resource_type_property]
      ws_url+="qp=#{options[:resource_type_property]}"
    end

    selected_resource = options[:selected_resource]
    if selected_resource
      options[:selected_id] = selected_resource.id
      options[:selected_text] = selected_resource.name(true)
    end

    select2_tag(name, ws_url, options)
  end

  #
  # Creates an enhanced dropdown selection box of components. Values are loaded on the fly via Ajax requests.
  # ==== Options
  # * <tt>:width</tt> - The width suffixed with unit, for example '300px' or '100%'. Default is '250px'
  # * <tt>:html_id</tt> - The id of the HTML element. Default is the name.
  # * <tt>:html_class</tt> - The class of the HTML element. Default is empty.
  # * <tt>:qualifiers</tt> - Array of resource qualifiers to filter.
  # * <tt>:resource_type_property</tt> -Filter on resource types on which the property is enabled, for example 'supportsGlobalDashboards'.
  # * <tt>:selected_resource</tt> - the resource that is selected by default.
  # * <tt>:placeholder</tt> - the label to display when nothing is selected
  # * <tt>:allow_clear</tt> - true if resource can be de-selected. Default is false.
  # * <tt>:open</tt> - true if the select-box must be open. Default is false.
  # * <tt>:select2_options</tt> - hash of select2 options
  #
  def component_select_tag(name, options={})
    # see limitation in /api/resources/search
    options[:min_length]=2

    ws_url="#{ApplicationController::root_context}/api/resources/search?f=s2&"
    if options[:qualifiers]
      ws_url+="q=#{options[:qualifiers].join(',')}"
    elsif options[:resource_type_property]
      ws_url+="qp=#{options[:resource_type_property]}"
    end

    # The WS should return component key instead of component id
    ws_url+='&display_key=true'

    selected_resource = options[:selected_resource]
    if selected_resource
      options[:selected_id]= "'" + selected_resource.key + "'"
      options[:selected_text] = selected_resource.name
    end

    select2_tag(name, ws_url, options)
  end

  #
  # Creates an enhanced dropdown selection box of users. Values are loaded on the fly via Ajax requests.
  # ==== Options
  # * <tt>:width</tt> - The width suffixed with unit, for example '300px' or '100%'. Default is '250px'
  # * <tt>:html_id</tt> - The id of the HTML element. Default is the name.
  # * <tt>:html_class</tt> - The class of the HTML element. Default is empty.
  # * <tt>:selected_user</tt> - the user that is selected by default.
  # * <tt>:placeholder</tt> - the label to display when nothing is selected
  # * <tt>:open</tt> - true if the select-box must be open. Default is false.
  # * <tt>:include_choices</tt> - choices that will be display when selecting the box
  # * <tt>:select2_options</tt> - hash of select2 options
  #
  def user_select_tag(name, options={})
    ws_url="#{ApplicationController::root_context}/api/users/search"
    options[:min_length]=2
    options[:select2_ajax_options]={
      'data' => 'function (term, page) { return { q: term, p: page } }',
      'results' => 'window.usersToSelect2'
    }

    user = options[:selected_user]
    if user
      # the login is a string so it have to be surrounded by quote to be taken in account by select2
      options[:selected_id]="'" + user.login + "'"
      options[:selected_text]=user.name + ' (' + user.login + ')'
    end

    select2_tag(name, ws_url, options)
  end

  #
  # Creates an enhanced dropdown selection box of metrics.
  # ==== Options
  # * <tt>:selected_key</tt> - the key of the metric that is selected by default.
  # * <tt>:multiple</tt> - If set to true the selection will allow multiple choices.
  # * <tt>:disabled</tt> - If set to true, the user will not be able to use this input.
  # * <tt>:allow_empty</tt> - If set to true, selecting a value is not mandatory
  # * <tt>:width</tt> - The width suffixed with unit, for example '300px' or '100%'. Default is '250px'
  # * <tt>:html_id</tt> - The id of the HTML element. Default is the name.
  # * <tt>:key_prefix</tt> - Prefix added to metric keys. Default is ''
  # * <tt>:extra_values</tt> -
  # * <tt>:placeholder</tt> - the label to display when nothing is selected
  #
  def metric_select_tag(name, metrics, options={})
    width=options[:width]||'250px'
    html_id=options[:html_id]||name
    js_options={
        'formatNoMatches' => "function(term){return '#{escape_javascript message('select2.noMatches')}'}",
        'width' => "'#{width}'"
    }

    select_tag_prompt=nil
    if options[:allow_empty]
      # add a cross icon to the select2 box
      js_options['placeholder']="''"
      js_options['allowClear']=true

      # add a select <option> with empty value
      select_tag_prompt=''
    end
    js_options.merge!(options[:select2_options]) if options[:select2_options]

    extra_values = options[:extra_values]
    metrics_by_domain={}
    if extra_values
      extra_values.inject(metrics_by_domain) do |h, extra_value|
        h['']||=[]
        h['']<<extra_value
        h
      end
    end
    js_options['placeholder']= "'#{options[:placeholder]}'" if options.has_key?(:placeholder)

    key_prefix = options[:key_prefix]||''
    metrics.sort_by(&:short_name).inject(metrics_by_domain) do |h, metric|
      domain=metric.domain||''
      h[domain]||=[]
      h[domain]<<[metric.short_name, key_prefix + metric.key]
      h
    end

    html = select_tag(name, grouped_options_for_select(metrics_by_domain, options[:selected_key], select_tag_prompt),
                      :multiple => options[:multiple],
                      :disabled => options[:disabled],
                      :id => html_id)
    js = "$j('##{html_id}').select2({#{js_options.map { |k, v| "#{k}:#{v}" }.join(',')}});"
    "#{html}<script>$j(document).ready(function() {#{js}});</script>"
  end

  # Since 3.6
  # Creates a dropdown selection box.
  #
  # ==== Options
  # * <tt>:width</tt> - The width suffixed with unit, for example '300px' or '100%'. Default is '250px'
  # * <tt>:placeholder</tt> - the label to display when nothing is selected. Default is ''.
  # * <tt>:allow_clear</tt> - true if value can be de-selected. Default is false.
  # * <tt>:show_search_box</tt> - true to display the search box. Default is false.
  # * <tt>:open</tt> - true to open the select-box. Default is false. Since 3.6.
  # * <tt>:select2_options</tt> - hash of select2 options
  #
  # ==== Example
  # dropdown_tag('user', [['Morgan', 'morgan'], ['Simon', 'simon']], {:show_search_box => false}, {:id => 'users_123'})
  #
  def dropdown_tag(name, option_tags, options={}, html_options={})
    width=options[:width]||'250px'
    html_id=html_options[:id]||name
    show_search_box=options[:show_search_box]||false
    minimumResultsForSearch=show_search_box ? 0 : option_tags.size + 1

    js_options={
        'minimumResultsForSearch' => minimumResultsForSearch,
        'allowClear' => options[:allow_clear]||false,
    }
    js_options['placeholder']= options.has_key?(:placeholder) ? "'#{options[:placeholder]}'" : "''"
    js_options['width']= "'#{width}'" if width
    js_options.merge!(options[:select2_options]) if options[:select2_options]

    html = select_tag(name, option_tags, html_options)
    js = "$j('##{html_id}').select2({#{js_options.map { |k, v| "#{k}:#{v}" }.join(',')}});"
    if options[:open]
      js += "$j('##{html_id}').select2('open');"
    end
    "#{html}<script>$j(document).ready(function() {#{js}});</script>"
  end

  # Creates an enhanced dropdown selection box of severities.
  # Options are the same as dropdown_tag()
  def severity_dropdown_tag(name, option_tags, options={}, html_options={})
    format_function = "function (state) {return \"<span class='sev_\" + state.id + \" withIcons'>\" + state.text + \"</span>\"}"
    options[:select2_options] = {:formatResult => format_function, :formatSelection => format_function}
    dropdown_tag(name, option_tags, options, html_options)
  end

  #
  # Creates a link linked to a POST action. A confirmation popup is opened when user clicks on the button.
  # ==== Options
  # * <tt>:id</tt> - HTML ID of the button
  # * <tt>:class</tt> - Additional CSS class, generally 'red-button' for deletions
  # * <tt>:confirm_button</tt> - L10n key of the confirmation button
  # * <tt>:confirm_title</tt> - L10n key of the confirmation title
  # * <tt>:confirm_msg</tt> - L10n key of the confirmation message
  # * <tt>:confirm_msg_params</tt> - Array of parameters used for building the confirmation message
  # * <tt>:confirm_width</tt> - width in pixels of the confirmation pop-up
  #
  def link_to_action(label, post_url, options={})
    clazz = options[:class]
    id = "id='#{options[:id]}'" if options[:id]
    title_key = options[:confirm_title]
    button_key = options[:confirm_button]
    message_key = options[:confirm_msg]
    message_params = options[:confirm_msg_params]
    width = options[:confirm_width]||500

    url = "#{ApplicationController.root_context}/confirm?url=#{u post_url}"
    url += "&tk=#{u title_key}" if title_key
    if message_key
      url += "&mk=#{u message_key}&"
      url += message_params.map { |p| "mp[]=#{u p}" }.join('&') if message_params
    end
    if button_key
      url += "&bk=#{u button_key}"
    end

    "<a href='#{url}' modal-width='#{width}' class='open-modal #{clazz}' #{id}>#{h label}</a>"
  end

  # Add a <tfoot> section to a table with pagination details and links.
  # Options :
  # :id HTML id of the <tfoot> node
  # :colspan number of columns in the table
  # :include_loading_icon add a hidden loading icon, only if value is true and if the option :id is set as well. The HTML id of the generated icon
  #    is '<id>_loading'
  def table_pagination(pagination, options={}, &block)
    html = '<tfoot'
    html += " id='#{options[:id]}'" if options[:id]
    html += "><tr><td"
    html += " colspan='#{options[:colspan]}'" if options[:colspan]
    html += '>'
    if options[:include_loading_icon] && options[:id]
      html += "<img src='#{ApplicationController.root_context}/images/loading-small.gif' style='display: none' id='#{options[:id]}_loading'>"
    end
    html += '<div'
    html += " id='#{options[:id]}_pages'" if options[:id]
    html += '>'
    html += message('x_results', :params => [pagination.count]) if pagination.count>0

    if pagination.pages > 1
      max_pages = pagination.pages
      current_page = pagination.page
      start_page = 1
      end_page = max_pages
      if max_pages > 20
        if current_page < 12
          start_page = 1
          end_page = 20
        elsif current_page > max_pages-10
          start_page = max_pages-20
          end_page = max_pages
        else
          start_page = current_page-10
          end_page = current_page+9
        end
      end

      html += ' | '
      if max_pages > 20 && start_page > 1
        html += (current_page!=1 ? yield(message('paging_first'), 1) : message('paging_first'))
        html += ' '
      end
      html += (pagination.previous? ? yield(message('paging_previous'), current_page-1) : message('paging_previous'))
      html += ' '
      for index in start_page..end_page
        html += (index != current_page ? yield(index.to_s, index) : index.to_s)
        html += ' '
      end
      html += (pagination.next? ? yield(message('paging_next'), current_page+1) : message('paging_next'))
      html += ' '
      if max_pages > 20 && end_page < max_pages
        html += (current_page != max_pages ? yield(message('paging_last'), max_pages) : message('paging_last'))
        html += ' '
      end
    end
    html += '</div></td></tr></tfoot>'
    html
  end


  # Add a <tfoot> section to a table with pagination details and links.
  # Options :
  # :id HTML id of the <tfoot> node
  # :colspan number of columns in the table
  # :url_results url to display on the number of results
  # :include_loading_icon add a hidden loading icon, only if value is true and if the option :id is set as well. The HTML id of the generated icon
  #    is '<id>_loading'
  def paginate_java(pagination, options={}, &block)
    total = pagination.total.to_i
    page_index = pagination.pageIndex() ? pagination.pageIndex().to_i : 1
    pages = pagination.pages().to_i
    results_html = options[:url_results] ? message('x_results', :params => "<a href='#{options[:url_results]}'>#{total}</a>") : message('x_results', :params => [total])

    html = '<tfoot'
    html += " id='#{options[:id]}'" if options[:id]
    html += "><tr><td"
    html += " colspan='#{options[:colspan]}'" if options[:colspan]
    html += '>'
    if options[:include_loading_icon] && options[:id]
      html += "<img src='#{ApplicationController.root_context}/images/loading-small.gif' style='display: none' id='#{options[:id]}_loading'>"
    end
    html += '<div'
    html += " id='#{options[:id]}_pages'" if options[:id]
    html += '>'
    html += results_html if total>0

    if pages > 1
      max_pages = pages
      current_page = page_index
      start_page = 1
      end_page = max_pages
      if max_pages > 20
        if current_page < 12
          start_page = 1
          end_page = 20
        elsif current_page > max_pages-10
          start_page = max_pages-20
          end_page = max_pages
        else
          start_page = current_page-10
          end_page = current_page+9
        end
      end

      html += ' | '
      if max_pages > 20 && start_page > 1
        html += (current_page!=1 ? yield(message('paging_first'), 1) : message('paging_first'))
        html += ' '
      end
      html += (page_index>1 ? yield(message('paging_previous'), current_page-1) : message('paging_previous'))
      html += ' '
      for index in start_page..end_page
        html += (index != current_page ? yield(index.to_s, index) : index.to_s)
        html += ' '
      end
      html += (page_index<pages ? yield(message('paging_next'), current_page+1) : message('paging_next'))
      html += ' '
      if max_pages > 20 && end_page < max_pages
        html += (current_page != max_pages ? yield(message('paging_last'), max_pages) : message('paging_last'))
        html += ' '
      end
    end
    html += '</div></td></tr></tfoot>'
    html
  end

  def url_for_issues(params)
    url = ApplicationController.root_context + '/issues/search#'
    params.each_with_index do |(key, value), index|
      if key == 'filter'
        key = 'id'
      end
      url += key.to_s + '=' + value.to_s
      if index < params.size - 1
        url += '|'
      end
    end
    url
  end

  def url_for_component_issues(component, params)
    if component.blank?
      url_for_issues(params)
    else
      url = ApplicationController.root_context + '/component_issues/index?id=' + url_encode(component.key) + '#'
      params.each_with_index do |(key, value), index|
        if key != 'componentUuids'
          url += key.to_s + '=' + value.to_s
          if index < params.size - 1
            url += '|'
          end
        end
      end
      url
    end
  end


  def get_issue_metrics
    ['violations', 'new_violations',
     'blocker_violations', 'critical_violations', 'major_violations', 'minor_violations', 'info_violations',
     'new_blocker_violations', 'new_critical_violations', 'new_major_violations', 'new_minor_violations', 'new_info_violations',
     'open_issues', 'reopened_issues', 'confirmed_issues', 'false_positive_issues']
  end

end

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
module ApplicationHelper

  # hack for firefox. The 'alt' parameter on images does not work. Firefox uses 'title' instead.
  # http://snippets.dzone.com/posts/show/2917
  def image_tag(location, options={})
    options[:title] ||= options[:alt]
    super(location, options)
  end

  def sonar_version
    Java::OrgSonarServerPlatform::Platform.getServer().getVersion()
  end

  # shortcut for the method is_admin?() without parameters. Result is kept in cache.
  def administrator?
    @is_administrator ||=
      begin
        is_admin?
      end
  end

  def qualifier_icon(object)
    qualifier=(object.respond_to?('qualifier') ? object.qualifier : object.to_s)
    if qualifier
      image_tag("q/#{qualifier}.png", :alt => Resourceable.qualifier_name(qualifier))
    else
      image_tag('e16.gif')
    end
  end

  def human_short_date(date)
    if Date.today - date.to_date == 0
      date.strftime('%H:%M')
    else
      l(date.to_date)
    end
  end


  # deprecated since 2.5. Use trend_icon() instead
  def tendency_icon(metric_or_measure, small=true, no_tendency_img=true)
    return trend_icon(metric_or_measure, {:big => !small})
  end

  def boolean_icon(boolean_value, options={})
    if boolean_value
      image_tag('tick.png')
    elsif options[:display_false]
      image_tag('cross.png')
    else
      nil
    end
  end

  def configuring?
    params['configuring']=='true'
  end

  def period_label(snapshot, period_index)
    return nil if snapshot.nil? || snapshot.project_snapshot.nil?
    mode=snapshot.project_snapshot.send "period#{period_index}_mode"
    mode_param=snapshot.project_snapshot.send "period#{period_index}_param"

    label=nil
    if mode
      if mode=='days'
        label = "%s days ago" % mode_param
      elsif mode=='version'
        label = "Version %s" % mode_param
      elsif mode=='previous_analysis'
        label = "Previous analysis (%s)" % localize(Date.parse(mode_param))
      elsif mode=='date'
        label = "Date: #{localize(Date.parse(mode_param))}"
      end
    end
    label
  end

  def html_measure(measure, metric_name=nil, show_alert_status=true, url=nil, suffix='', small=true, no_tendency_img=false)
    html=''

    if measure && measure.metric
      link_rel=''
      show_link= !url.nil?

      if measure.metric.val_type==Metric::VALUE_TYPE_LEVEL
        html=image_tag("levels/#{measure.data.downcase}.png") unless measure.data.blank?
      else
        html=measure.formatted_value
      end

      alert_class=''
      alert_link = false
      style=''
      if show_alert_status && !measure.alert_status.blank?
        alert_class="class='alert_#{measure.alert_status}'" unless measure.metric.val_type==Metric::VALUE_TYPE_LEVEL
        link_rel=h(measure.alert_text)
        show_link=true
        alert_link=true
      elsif measure.metric.val_type==Metric::VALUE_TYPE_RATING && measure.color
        style = "style='background-color: #{measure.color.html};padding: 2px 5px'"
      end

      html="<span id='m_#{measure.key}' #{alert_class} #{style}>#{html}</span>"
      if metric_name
        html="#{html} #{metric_name}"
      end

      if show_link
        if url.blank?
          url='#'
          link_class='nolink'
        else
          link_class=''
        end
        html="<a href='#{url}' style='#{alert_link ? "cursor : default" : ""}' class='#{link_class}' rel='#{link_rel}' title='#{link_rel}'>#{html}</a>"
      end
      no_tendency_img=true if (measure.metric.val_type==Metric::VALUE_TYPE_LEVEL || measure.metric.val_type==Metric::VALUE_TYPE_BOOLEAN || measure.metric.val_type==Metric::VALUE_TYPE_RATING)
      html="#{html} #{tendency_icon(measure, small, no_tendency_img)} #{suffix}"
    end
    html
  end

  def configuration(key, default = nil)
    prop_value = controller.java_facade.getContainer().getComponent(Java::OrgApacheCommonsConfiguration::Configuration.java_class).getProperty(key)
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
  #   url_for_static(:path => 'images/sonar.png')
  #   url_for_static(:plugin => 'myplugin', :path => 'image.png')
  def url_for_static(options={})
    if options[:plugin]
      "#{ApplicationController.root_context}/static/#{options[:plugin]}/#{options[:path]}"
    else
      "#{ApplicationController.root_context}/#{options[:path]}"
    end
  end

  def url_for_gwt(page)
    "#{ApplicationController.root_context}/plugins/home/#{page}"
  end

  # URL to GWT page for a given resource.
  #
  # === Optional parameters
  #
  # * <tt>:resource</tt> - id or key of the selected resource. Default value is the current resource.
  #
  # === Examples
  #
  #   url_for_resource_gwt('org.sonar.tests:reference/org.sonar.plugins.core.hotspots.GwtHotspots')
  #   url_for_resource_gwt('org.sonar.tests:reference/org.sonar.plugins.core.hotspots.GwtHotspots', :resource => 'org.apache.struts:struts-parent')
  def url_for_resource_gwt(page, options={})
    if options[:resource]
      "#{ApplicationController.root_context}/plugins/resource/#{options[:resource]}?page=#{page}"
    elsif @project
      "#{ApplicationController.root_context}/plugins/resource/#{@project.id}?page=#{page}"
    else
      ''
    end
  end

  # URL to measures drilldown page for a given metric.
  #
  # === Optional parameters
  #
  # * <tt>:resource</tt> - id or key of the selected resource
  # * <tt>:highlight</tt> - key of the metric to highlight, different than the metric to drilldown.
  #  *<tt>:viewer_plugin_key</tt> - the default GWT plugin to be used when clicking on a resource in the drilldown to view it's source
  #
  # === Examples
  #
  #   url_for_drilldown('ncloc')
  #   url_for_drilldown('ncloc', {:resource => 'org.apache.struts:struts-parent', :highlight => 'lines'})
  #   url_for_drilldown('ncloc', {:resource => 'org.apache.struts:struts-parent', :viewer_plugin_key => 'org.sonar.plugins.core.violationsviewer.GwtViolationsViewer'})
  def url_for_drilldown(metric_or_measure, options={})
    if metric_or_measure.is_a? ProjectMeasure
      metric_key = metric_or_measure.metric.key
    elsif metric_or_measure.is_a? Metric
      metric_key = metric_or_measure.key
    else
      metric_key = metric_or_measure
    end

    if options[:resource]
      url_for(:controller => 'drilldown', :action => 'measures', :id => options[:resource], :metric => metric_key, :highlight => options[:highlight], :viewer_plugin_key => options[:viewer_plugin_key])
    elsif @project
      url_for(:controller => 'drilldown', :action => 'measures', :id => @project.id, :metric => metric_key, :highlight => options[:highlight], :viewer_plugin_key => options[:viewer_plugin_key])
    else
      ''
    end
  end

  #
  # Display a measure
  #
  # === Optional parameters
  # * <tt>:skip_span_id</tt> - skip the generation of the html attribute 'id'. Default is false
  # * <tt>:url</tt> - add an url on the measure.
  # * <tt>:prefix</tt> - add a prefix. Default is ''.
  # * <tt>:suffix</tt> - add a suffix. Default is ''.
  # * <tt>:default</tt> - text to return if metric or measure not found. Default is blank string.
  #
  # === Examples
  #
  #   format_measure('ncloc')
  #   format_measure('ncloc', {:suffix => '', :url => url_for_drilldown('ncloc'), :default => '-'})
  #
  def format_measure(metric_or_measure, options={})
    html=''

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

      if m.metric.val_type==Metric::VALUE_TYPE_LEVEL
        html=image_tag("levels/#{m.data.downcase}.png") unless m.data.blank?
      else
        html=m.formatted_value
      end

      alert_class=''
      alert_link = false
      style = ''
      if !(m.alert_status.blank?)
        alert_class="class='alert_#{m.alert_status}'" unless m.metric.val_type==Metric::VALUE_TYPE_LEVEL
        link_rel=h(m.alert_text)
        show_link=true
        alert_link = true

      elsif m.metric.val_type==Metric::VALUE_TYPE_RATING && m.color
        style = "style='background-color: #{m.color.html};padding: 2px 5px'"
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
        html="<a href='#{url}' style='#{alert_link ? "cursor : default" : ""}' class='#{link_class}' rel='#{link_rel}' title='#{link_rel}'>#{html}</a>"
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
    if resource.display_dashboard?
      if options[:dashboard]
        link_to(name || resource.name, {:overwrite_params => {:controller => 'dashboard', :action => 'index', :id => resource.copy_resource_id || resource.id}}, :title => options[:title])
      else
        # stay on the same page (for example components)
        link_to(name || resource.name, {:overwrite_params => {:id => resource.copy_resource_id || resource.id}}, :title => options[:title])
      end
    else
      link_to(name || resource.name, {:controller => 'resource', :action => 'index', :id => resource.id}, :popup => ['resource', 'height=800,width=900,scrollbars=1,resizable=1'], :title => options[:title])
    end
  end


  #
  #
  # JFree Eastwood is a partial implementation of Google Chart Api
  #
  #
  def gchart(parameters, options={})
    image_tag("#{ApplicationController.root_context}/gchart?#{parameters}", options)
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
  #
  def barchart(options)
    percent = (options[:percent] || 100).to_i
    width=(options[:width] || 150).to_i
    if options[:positive_color] && percent>0
      color = options[:positive_color]
    elsif options[:negative_color] && percent<0
      color = options[:negative_color]
    else
      color = options[:color]||'#777'
    end

    align=(percent<0 ? 'float: right;' : nil)
    "<div class='barchart' style='width: #{width}px'><div style='width: #{percent.abs}%;background-color:#{color};#{align}'></div></div>"
  end

  def chart(parameters, options={})
    image_tag("#{ApplicationController.root_context}/chart?#{parameters}", options)
  end

  def link_to_favourite(resource, options={})
    return '' unless (logged_in?)
    return '' if resource.nil?
    resource_id=(resource.is_a?(Fixnum) ? resource : (resource.copy_resource_id || resource.id))
    html_id=options['html_id'] || "fav#{resource_id}"
    initial_class='notfav'
    initial_tooltip='Click to add to favourites'
    if current_user.favourite?(resource_id)
      initial_class='fav'
      initial_tooltip='Click to remove from favourites'
    end

    link_to_remote('', :url => { :controller => 'favourites', :action => 'toggle', :id => resource_id, :elt => html_id},
      :method => :post, :html => {:class => initial_class, :id => html_id, :alt => initial_tooltip, :title => initial_tooltip})
  end

  #
  #
  # Display the trend icon :
  #
  # === Optional parameters
  # * empty: true|false. Show an empty transparent image when no trend or no measure. Default is false.
  #
  # === Examples
  # trend_icon('ncloc')
  # trend_icon(measure('ncloc'))
  # trend_icon('ncloc', :big => true, :empty => true)
  #
  def trend_icon(metric_or_measure, options={})
    if metric_or_measure.is_a? ProjectMeasure
      m = metric_or_measure
    elsif @snapshot
      m = @snapshot.measure(metric_or_measure)
    end

    if m.nil? || m.tendency.nil? || m.tendency==0
      return options[:empty] ? image_tag("transparent.gif", :width => "16", :alt => "") : nil
    end
    filename = m.tendency.to_s

    case m.tendency_qualitative
    when 0
      filename+= '-black'
    when -1
      filename+= '-red'
    when 1
      filename+= '-green'
    end
    image_tag("tendency/#{filename}-small.png")
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
  # variation_value('ncloc', :index => 3)
  #
  def variation_value(metric_or_measure, options={})
    if metric_or_measure.is_a?(ProjectMeasure)
      m = metric_or_measure
    elsif @snapshot
      m = @snapshot.measure(metric_or_measure)
    end

    index=options[:index]
    if index.nil? && defined?(@dashboard_configuration) && @dashboard_configuration.selected_period?
      index = @dashboard_configuration.period_index
    end

    m.variation(index)
  end


  #
  #
  # Format variation value
  #
  # === Optional parameters
  # * color: true|false. Default is true.
  # * index: integer between 1 and 5. By default the index is defined by the dashboard variation select-box
  #
  # === Examples
  # format_variation('ncloc')
  # format_variation(measure('ncloc'), :index => 3, :color => true)
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
        formatted_val=(val>=0 ? "+#{m.format_numeric_value(val)}" : m.format_numeric_value(val))
        css_class=''
        if options[:color]||true
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
        end
        html="<span class='#{css_class}'>(#{formatted_val})</span>"
      end
    end
    html
  end
end

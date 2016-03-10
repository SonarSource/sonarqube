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
include ERB::Util
module DashboardHelper
  include WidgetPropertiesHelper
  include MetricsHelper
  include MeasuresHelper

  def dashboard_action(action_name, opts={})
    if @resource
      { :action => action_name, :did => @dashboard.id, :id => @resource.id }.merge!(opts)
    else
      { :action => action_name, :did => @dashboard.id }.merge!(opts)
    end
  end

  def add_category_to_url(url)
    url + (url.include?('?') ? '&' : '?') + 'category='
  end

  def formatted_value(measure, default='')
    measure ? measure.formatted_value : default
  end

  def measure(metric_key)
    @snapshot.measure(metric_key)
  end

  def period_select_option_tags(snapshot, html_class = '')
    selected=(!params[:period] || params[:period] == '0' ? 'selected' : '')
    options = "<option #{selected} value='0' class='#{html_class}'/>#{message('time_changes')}...</option>"
    period_options = ''
    (1..5).each { |index|
      option = period_select_options(snapshot, index, html_class)
      if option
        period_options += option
      end
    }
    if !period_options.empty?
      options += period_options
    else
      nil
    end
  end

  def period_select_options(snapshot, index, html_class = '')
    label = period_label(snapshot, index)
    if label && snapshot.period_datetime(index)
      selected=(params[:period]==index.to_s ? 'selected' : '')
      "<option value='#{index}' #{selected} class='#{html_class}'>&Delta; #{h(label)}</option>"
    else
      nil
    end
  end

  def period_label(snapshot, index)
    if snapshot.project_snapshot
      mode = snapshot.period_mode(index)
      mode_param = snapshot.period_param(index)
      date = localize(snapshot.period_datetime(index).to_date) if snapshot.period_datetime(index)
      Api::Utils.java_facade.getPeriodLabel(mode, mode_param, date) if mode
    end
  end

  def short_period_label(snapshot, index)
    if snapshot.project_snapshot
      Api::Utils.java_facade.getPeriodLabel(index)
    end
  end

  def violation_period_select_options(snapshot, index)
    return nil if snapshot.nil? || snapshot.project_snapshot.nil?
    mode=snapshot.project_snapshot.send "period#{index}_mode"
    mode_param=snapshot.project_snapshot.send "period#{index}_param"
    date=snapshot.project_snapshot.send "period#{index}_date"

    if mode
      if mode=='days'
        label = message('added_over_x_days', :params => mode_param.to_s)
      elsif mode=='version'
        label = message('added_since_version', :params => mode_param.to_s)
      elsif mode=='previous_analysis'
        if !date.nil?
          label = message('added_since_previous_analysis_detailed', :params => date.strftime('%Y %b. %d').to_s)
        else
          label = message('added_since_previous_analysis')
        end
      elsif mode=='previous_version'
        unless mode_param.nil?
          label = message('added_since_previous_version_detailed', :params => mode_param.to_s)
        else
          label = message('added_since_previous_version')
        end
      elsif mode=='date'
        label = message('added_since', :params => date.strftime('%Y %b %d').to_s)
      end
      if label
        selected=(params[:period]==index.to_s ? 'selected' : '')
        "<option value='#{index}' #{selected}>#{label}</option>"
      end
    else
      nil
    end

  end

  def measure_or_variation_value(measure)
    if measure
      @period_index ? measure.variation(@period_index) : measure.value
    else
      nil
    end
  end

  def switch_to_widget_resource(widget)
    @backup_resource=@resource
    @backup_project=@project
    @backup_snapshot=@snapshot
    @backup_dashboard_configuration=@dashboard_configuration

    if widget.resource_id
      widget_resource = Project.find_by_id(widget.resource_id)
      if widget_resource
        @resource = widget_resource
        @project = @resource
        @snapshot=@resource.last_snapshot
        @dashboard_configuration=Api::DashboardConfiguration.new(@dashboard, :period_index => params[:period], :snapshot => @snapshot)
      end
    end
  end

  def restore_global_resource
    @resource=@backup_resource
    @project=@backup_project
    @snapshot=@backup_snapshot
    @dashboard_configuration=@backup_dashboard_configuration
    @widget_title=nil
  end

  def widget_title(widget)
    resource_name=link_to(h(@resource.name), {:controller => 'dashboard', :action => 'index', :id => @resource.id}) if @resource && @dashboard.global && !widget.java_definition.global

    [resource_name, @widget_title].compact.join(' - ')
  end

  def widget_body(widget)
    widget_body=nil

    if widget.configured
      begin
        if has_role?(:user, @resource)
          widget_body=render :inline => widget.java_definition.getTarget().getTemplate(), :locals => {:widget_properties => widget.properties_as_hash, :widget => widget, :dashboard_configuration => @dashboard_configuration}
        else
          widget_body=message 'not_authorized_to_access_project', h(@resource.name)
        end
      rescue => error
        logger.error(message('dashboard.cannot_render_widget_x', :params => [widget.java_definition.getId(), error]), error)
      end
    end

    widget_body
  end

end

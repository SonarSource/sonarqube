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
class WidgetController < ApplicationController
  helper :dashboard

  SECTION=Navigation::SECTION_RESOURCE

  def index
    load_resource
    load_widget
    params[:layout]='false'
    render :action => 'index'
  end

  def show
    load_resource
    load_widget
    begin
      render :inline => @widget_definition.getTarget().getTemplate(), :locals => {
          :widget_properties => @widget.properties_as_hash, :widget => @widget, :dashboard_configuration => @dashboard_configuration
      }
    rescue => error
      logger.error(message('dashboard.cannot_render_widget_x', :params => [@widget_definition.getId(), error]), error)
      render :status => 500
    end

  end

  private

  def load_resource
    if params[:resource]
      @resource=Project.by_key(params[:resource])
      not_found("Resource not found") unless @resource
      access_denied unless has_role?(:user, @resource)

      @project=@resource
      @snapshot = @resource.last_snapshot
    end
  end

  def load_widget
    widget_key=params[:id]
    @widget_definition = java_facade.getWidget(widget_key)
    not_found('Unknown widget') unless @widget_definition

    authorized=(@widget_definition.getUserRoles().size==0)
    unless authorized
      @widget_definition.getUserRoles().each do |role|
        authorized=(role=='user') || (role=='viewer') || has_role?(role, @resource)
        break if authorized
      end
    end
    access_denied unless authorized

    @widget=Widget.new(:widget_key => widget_key)
    @widget.id=1
    @widget_definition.getWidgetProperties().each do |property_definition|
      value = params[property_definition.key()]
      @widget.properties<<WidgetProperty.new(
        :widget => @widget,
        :kee => property_definition.key(),
        :text_value => (value.blank? ? property_definition.defaultValue : value)
      )
    end
    @dashboard_configuration=Api::DashboardConfiguration.new(nil, :period_index => params[:period], :snapshot => @snapshot)
    @widget_width = params[:widget_width] || '350px'
  end
end

#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2011 SonarSource
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
class WidgetController < ApplicationController
  helper :dashboard

  SECTION=Navigation::SECTION_RESOURCE

  def index
    begin
      load_widget
      load_resource
      params[:layout]='false'
      render :action => 'index'

    rescue Exception => e
      render :text => e
    end
  end

  private

  def load_resource
    @resource=Project.by_key(params[:resource])
    @project=@resource
    return access_denied unless has_role?(:user, @resource)
    @snapshot = @resource.last_snapshot
  end

  def load_widget
    widget_key = params[:id]
    @widget_definition = java_facade.getWidget(widget_key)
    @widget=Widget.new(:widget_key => widget_key, :id => 1)
    
    @widget_definition.getWidgetProperties().each do |property_definition|
      @widget.properties<<WidgetProperty.new(
          :kee => property_definition.key(),
          :value_type => property_definition.type().toString(),
          :text_value => params[property_definition.key()] || property_definition.defaultValue
      )
    end

    @dashboard_configuration=Api::DashboardConfiguration.new(nil, :period_index => params[:period], :snapshot => @snapshot)
  end
end
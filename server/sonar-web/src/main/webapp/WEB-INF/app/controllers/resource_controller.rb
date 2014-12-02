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

class ResourceController < ApplicationController

  SECTION=Navigation::SECTION_RESOURCE

  helper :dashboard
  helper UsersHelper, IssuesHelper

  def index
    require_parameters 'id'

    component_key = params[:id]
    if Api::Utils.is_number?(component_key)
      component=Project.by_key(component_key)
      not_found unless component
      access_denied unless has_role?(:user, component)
      component_key = component.key
    end

    anchor = "component=#{component_key}"
    anchor += "&tab=#{params[:tab]}" if params[:tab]
    redirect_to url_for(:controller => 'component', :action => 'index') + '#' + anchor
  end

  # deprecated stuff for drilldown
  def view
    require_parameters 'id'
    @resource = Project.by_key(params[:id])
    access_denied unless has_role?(:user, @resource)
    @snapshot = @resource.last_snapshot
    load_extensions() if @snapshot
    if @extension
      render :partial => 'view'
    else
      not_found('Extension not found')
    end
  end

  #
  # Call by new component viewer to display plugin extension
  #
  # GET /resource/extension?id=<component_key>&tab=extension_key
  def extension
    @resource = Project.by_key(params[:id])
    not_found('Resource not found') unless @resource
    @resource = @resource.permanent_resource
    access_denied unless has_role?(:user, @resource)

    @snapshot = @resource.last_snapshot
    load_extensions() if @snapshot
    if @extension
      render :partial => 'extension'
    else
      not_found('Extension not found')
    end
  end

  private

  def load_extensions
    @extensions=[]
    java_facade.getResourceTabs(@resource.scope, @resource.qualifier, @resource.language, @snapshot.metric_keys.to_java(:string)).each do |tab|
      if tab.getUserRoles().empty?
        @extensions<<tab
      else
        tab.getUserRoles().each do |role|
          if has_role?(role, @resource)
            @extensions<<tab
            break
          end
        end
      end
    end

    if params[:tab].present?
      # Hack to manage violations as issues.
      params[:tab] = 'issues' if params[:tab] == 'violations'
      @extension=@extensions.find { |extension| extension.getId()==params[:tab] }

    elsif !params[:metric].blank?
      metric = Metric.by_key(params[:metric])
      @extension=@extensions.find { |extension| extension.getDefaultTabForMetrics().include?(metric.key) }
    end
    @extension=@extensions.find { |extension| extension.isDefaultTab() } if @extension==nil
  end

end

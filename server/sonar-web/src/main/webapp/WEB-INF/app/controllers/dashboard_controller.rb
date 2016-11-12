#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2016 SonarSource
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
class DashboardController < ApplicationController

  SECTION=Navigation::SECTION_RESOURCE

  def index
    if params[:id]
      @resource = Project.by_key(params[:id])
      return project_not_found unless @resource
      @resource = @resource.permanent_resource

      access_denied unless has_role?(:user, @resource)

      # for backward compatibility with old widgets
      @project = @resource

      # if file
      if !@resource.display_dashboard?
        @snapshot = @resource.last_snapshot
        return project_not_analyzed unless @snapshot
        @hide_sidebar = true
        @file = @resource
        @project = @resource.root_project
        @metric=params[:metric]
        render :action => 'no_dashboard'
      else
        # it is a project dashboard
        # if governance plugin is installed and we are opening a view
        if Project.root_qualifiers.include?('VW') && (@resource.qualifier == 'VW' || @resource.qualifier == 'SVW')
          return redirect_to(url_for({:controller => 'governance'}) + '?id=' + url_encode(params[:id]))
        else
          @snapshot = @resource.last_snapshot
          render :action => 'overview'
        end
      end
    else
      if logged_in?
        return redirect_to :controller => 'projects', :action => 'favorite'
      else
        return redirect_to :controller => 'projects'
      end
    end
  end

  private

  def project_not_found
    flash[:error] = message('dashboard.project_not_found')
    redirect_to :action => :index
  end

  def project_not_analyzed
    render :action => 'empty'
  end
end

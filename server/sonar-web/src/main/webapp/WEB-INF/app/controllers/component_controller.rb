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
class ComponentController < ApplicationController

  SECTION=Navigation::SECTION_RESOURCE

  def index
    if params[:id]
      @resource=Project.by_key(params[:id])
      return project_not_found unless @resource
      @resource=@resource.permanent_resource
    end

    @line = params[:line]

    if request.xhr?
      render :action => 'index'
    else
      # popup mode, title will always be displayed
      params[:layout] = 'false'
      render :action => 'index'
    end
  end

  private

  def project_not_found
    flash[:error] = message('dashboard.project_not_found')
    redirect_to :controller => 'dashboard', :action => 'index'
  end

end

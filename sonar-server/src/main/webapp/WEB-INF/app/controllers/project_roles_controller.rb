#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2013 SonarSource
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
class ProjectRolesController < ApplicationController
  helper RolesHelper

  SECTION=Navigation::SECTION_RESOURCE

  verify :method => :post, :only => [:grant_users, :grant_groups ], :redirect_to => { :action => 'index' }

  def index
    @project=Project.by_key(params[:id])
    access_denied unless is_admin?(@project)
  end

  def edit_users
    @project=Project.by_key(params[:id])
    access_denied unless is_admin?(@project)
    @role = params[:role]
  end

  def edit_groups
    @project=Project.by_key(params[:id])
    access_denied unless is_admin?(@project)
    @role = params[:role]
  end

  def grant_users
    project=Project.by_key(params[:id])
    access_denied unless is_admin?(project)

    UserRole.grant_users(params[:users], params[:role], project.id)
    redirect_to(:action => 'index', :id => project.id)
  end

  def grant_groups
    project=Project.by_key(params[:id])
    access_denied unless is_admin?(project)

    GroupRole.grant_groups(params[:groups], params[:role], project.id)
    redirect_to(:action => 'index', :id => project.id)
  end

end

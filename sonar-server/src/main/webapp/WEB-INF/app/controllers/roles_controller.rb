#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
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
class RolesController < ApplicationController
  helper RolesHelper
  
  SECTION=Navigation::SECTION_CONFIGURATION
  
  before_filter :admin_required
  verify :method => :post, :only => [:grant_users, :grant_groups ], :redirect_to => { :action => 'global' }

  def global
  end

  def projects
    @projects=Project.find(:all,
      :conditions => {:enabled=>true, :scope => Project::SCOPE_SET, :qualifier => [Project::QUALIFIER_VIEW, Project::QUALIFIER_SUBVIEW, Project::QUALIFIER_PROJECT]},
      :include => ['user_roles', 'group_roles']).sort{|a,b| a.name.downcase<=>b.name.downcase}
  end

  def edit_users
    @project=Project.by_key(params[:resource]) if !params[:resource].blank?
    @role = params[:role]
  end

  def edit_groups
    @project=Project.by_key(params[:resource]) if !params[:resource].blank?
    @role = params[:role]
  end

  def grant_users
    UserRole.grant_users(params[:users], params[:role], params[:resource])
    redirect
  end
  
  def grant_groups
    GroupRole.grant_groups(params[:groups], params[:role], params[:resource])
    redirect
  end

  private
  def redirect
    redirect_to(:action => params['redirect'] || 'global' )
  end
end

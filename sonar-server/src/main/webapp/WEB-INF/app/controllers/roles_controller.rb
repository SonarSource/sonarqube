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
  PER_PAGE = 2

  before_filter :admin_required
  verify :method => :post, :only => [:grant_users, :grant_groups], :redirect_to => {:action => 'global'}

  def global
  end

  def projects
    # for backward-compatibility with versions of views plugin that do not depend on sonar 2.15
    if java_facade.hasPlugin('views')
      @qualifiers = (['VW', 'SVW'] + java_facade.getQualifiersWithProperty('hasRolePolicy').to_a).compact.uniq
    else
      @qualifiers = java_facade.getQualifiersWithProperty('hasRolePolicy')
    end
    @qualifier = params[:qualifier] || 'TRK'

    conditions_sql = 'projects.enabled=:enabled and projects.qualifier=:qualifier and projects.copy_resource_id is null'
    conditions_values = {:enabled => true, :qualifier => @qualifier}

    if params[:q].present?
      conditions_sql += ' and projects.id in (select ri.resource_id from resource_index ri where ri.qualifier=:qualifier and ri.kee like :search)'
      conditions_values[:search]="#{params[:q].downcase}%"
    end

    @pagination = Api::Pagination.new(params.merge(:per_page => 50))
    @projects=Project.find(:all,
                           :include => %w(user_roles group_roles index),
                           :conditions => [conditions_sql, conditions_values],
                           :order => 'resource_index.kee',
                           :offset => @pagination.offset,
                           :limit => @pagination.limit)
    @pagination.count=Project.count(:conditions => [conditions_sql, conditions_values])
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
    redirect_to(:action => params['redirect'] || 'global')
  end
end

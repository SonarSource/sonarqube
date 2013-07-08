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

#
# @since 3.7
#
# Note : do NOT use @template as an instance variable
# as it is a reserved variable in Rails
#
class PermissionTemplatesController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION

  before_filter :admin_required

  #
  # GET
  #
  def index
    @permission_templates = Internal.permission_templates.selectAllPermissionTemplates
  end

  def edit_users
  end

  def edit_groups
  end

  def create_form
    render :partial => 'permission_templates/permission_template_form',
           :locals => {:form_action => 'create', :message_title => 'new_template', :message_submit => 'create_template'}
  end

  #
  # POST
  #
  def create
    verify_post_request
    @permission_template = Internal.permission_templates.createPermissionTemplate(params[:name], params[:description])
    redirect_to :action => 'index'
  end

  def edit_form
    @permission_template = Internal.permission_templates.selectPermissionTemplate(params[:name])
    render :partial => 'permission_templates/permission_template_form',
           :locals => {:form_action => 'edit', :message_title => 'edit_template', :message_submit => 'edit_template'}
  end

  #
  # POST
  #
  def edit
    verify_post_request
    require_parameters :id, :name
    Internal.permission_templates.updatePermissionTemplate(params[:id].to_i, params[:name], params[:description])
    redirect_to :action => 'index'
  end

  def delete_form
    @permission_template = Internal.permission_templates.selectPermissionTemplate(params[:name])
    render :partial => 'permission_templates/delete_form'
  end

  #
  # POST
  #
  def delete
    verify_post_request
    require_parameters :id
    Internal.permission_templates.deletePermissionTemplate(params[:id].to_i)
    redirect_to :action => 'index'
  end

end

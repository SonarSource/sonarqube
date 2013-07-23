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

  helper RolesHelper
  include RolesHelper

  SECTION = Navigation::SECTION_CONFIGURATION

  before_filter :admin_required

  #
  # GET
  #
  def index
    all_templates = Internal.permission_templates.selectAllPermissionTemplates

    @permission_templates = get_templates_and_permissions(all_templates)
    @root_qualifiers = get_root_qualifiers
    @default_templates = get_default_templates_per_qualifier(@root_qualifiers)
  end

  #
  # GET (modal form)
  #
  def edit_users
    @permission = params[:permission]
    @permission_template = Internal.permission_templates.selectPermissionTemplate(params[:name])
    @users_with_permission = @permission_template.getUsersForPermission(params[:permission]).collect {|u| [u.userName, u.userLogin]}
    @users_without_permission = all_users.each.collect {|u| [u.name, u.login]} - @users_with_permission

    render :partial => 'permission_templates/edit_users'
  end

  #
  # GET (modal form)
  #
  def edit_groups
    @permission = params[:permission]
    @permission_template = Internal.permission_templates.selectPermissionTemplate(params[:name])
    @groups_with_permission = @permission_template.getGroupsForPermission(params[:permission]).collect {|g| [group_ref(g.groupName), group_ref(g.groupName)]}
    @groups_without_permission = all_groups.each.collect {|g| g.nil? ? ['Anyone', 'Anyone'] : [g.name, g.name]} - @groups_with_permission

    render :partial => 'permission_templates/edit_groups'
  end

  #
  # POST
  #
  def update_users_permissions
    verify_post_request
    require_parameters :name, :permission

    @permission_template = Internal.permission_templates.selectPermissionTemplate(params[:name])

    selected_users = params[:users] || []

    previous_users_with_permission = @permission_template.getUsersForPermission(params[:permission]).collect {|u| [u.userName, u.userLogin]}
    new_users_with_permission = all_users.select {|u| selected_users.include?(u.login)}.collect {|u| [u.name, u.login]}

    promoted_users = new_users_with_permission - previous_users_with_permission
    demoted_users = previous_users_with_permission - new_users_with_permission

    promoted_users.each do |user|
      Internal.permission_templates.addUserPermission(params[:name], params[:permission], user[1])
    end

    demoted_users.each do |user|
      Internal.permission_templates.removeUserPermission(params[:name], params[:permission], user[1])
    end

    redirect_to :action => 'index'
  end

  #
  # POST
  #
  def update_groups_permissions
    verify_post_request
    require_parameters :name, :permission

    @permission_template = Internal.permission_templates.selectPermissionTemplate(params[:name])

    selected_groups = params[:groups] || []

    previous_groups_with_permission = @permission_template.getGroupsForPermission(params[:permission]).collect {|g| [group_ref(g.groupName), group_ref(g.groupName)]}
    new_groups_with_permission = all_groups.collect {|g| g.nil? ? ['Anyone', 'Anyone'] : [g.name, g.name]}.select {|g| selected_groups.include?(g[1])}

    promoted_groups = new_groups_with_permission - previous_groups_with_permission
    demoted_groups = previous_groups_with_permission - new_groups_with_permission

    promoted_groups.each do |group|
      Internal.permission_templates.addGroupPermission(params[:name], params[:permission], group[1])
    end

    demoted_groups.each do |group|
      Internal.permission_templates.removeGroupPermission(params[:name], params[:permission], group[1])
    end

    redirect_to :action => 'index'
  end

  #
  # GET (modal form)
  #
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

  #
  # GET (modal form)
  #
  def edit_form
    @permission_template = Internal.permission_templates.selectPermissionTemplate(params[:name])
    render :partial => 'permission_templates/permission_template_form',
           :locals => {:form_action => 'edit', :message_title => 'edit_template', :message_submit => 'update_template'}
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

  #
  # GET (modal form)
  #
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

  #
  # GET (modal form)
  #
  def default_templates_form
    all_templates = Internal.permission_templates.selectAllPermissionTemplates

    @permission_templates_options = all_templates.sort_by {|t| t.name.downcase}.collect {|t| [t.name, t.key]}
    @root_qualifiers = get_root_qualifiers
    @default_templates = get_default_templates_per_qualifier(@root_qualifiers)

    render :partial => 'permission_templates/default_templates_form'
  end

  #
  # POST
  #
  def update_default_templates
    verify_post_request
    get_root_qualifiers.each do |qualifier|
      Property.set("sonar.permission.template.#{qualifier}.default", params["default_template_#{qualifier}"])
      if 'TRK' == qualifier
        Property.set("sonar.permission.template.default", params["default_template_#{qualifier}"])
      end
    end
    redirect_to :action => 'index'
  end


  private

  def get_root_qualifiers
    Java::OrgSonarServerUi::JRubyFacade.getInstance().getResourceRootTypes().map {|type| type.getQualifier()}.sort
  end

  def get_default_templates_per_qualifier(root_qualifiers)
    default_templates = {}
    default_template_property = Property.by_key("sonar.permission.template.default")
    root_qualifiers.each do |qualifier|
      qualifier_template = Property.by_key("sonar.permission.template.#{qualifier}.default")
      default_templates[qualifier] = qualifier_template ? qualifier_template.text_value : default_template_property.text_value
    end
    default_templates
  end

  def get_templates_and_permissions(permission_templates)
    templates_names = permission_templates.collect {|t| t.name}
    permission_templates = []
    templates_names.each do |template_name|
      permission_templates << Internal.permission_templates.selectPermissionTemplate(template_name)
    end
    permission_templates.sort_by {|t| t.name.downcase}
  end

end

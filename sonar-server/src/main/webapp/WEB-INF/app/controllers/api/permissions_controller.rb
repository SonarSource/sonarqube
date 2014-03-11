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

#
# Since 3.7
#
class Api::PermissionsController < Api::ApiController

  #
  # POST /api/permissions/add
  #
  # -- Mandatory parameters
  # 'permission' is the key of the permission to add.
  #   For global permissions, available values are : admin, profileadmin, shareDashboard, scan, dryRunScan, provisioning.
  #   For component permissions, available values are : user, codeviewer, admin, issueadmin.
  # 'user' is the user identifier (login)
  # OR
  # 'group' is the group identifier (group name or 'anyone')
  #
  # -- Optional parameters
  # 'component' is the component key on which add the permission. If null, the permission should be a global permission.
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/permissions/add?permission=shareDashboard&user=new_user'
  #
  # -- Notes
  # An exception will be raised if both a user and a group are provided
  # Requests that attempt to add an already configured permission will be silently ignored
  #
  # since 3.7
  # 'component' parameter and 'provisioning' permission have been added in 4.0
  # 'issueadmin' permission has been added in 4.1
  #
  def add
    verify_post_request
    require_parameters :permission
    require_one_of :group, :user
    Internal.permissions.addPermission(params)
    hash = {:user => params[:user], :group => params[:group], :permission => params[:permission]}
    respond_to do |format|
      format.json { render :json => jsonp(hash), :status => 200 }
      format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'sonar', :status => 200) }
    end
  end

  #
  # POST /api/permissions/remove
  #
  # -- Mandatory parameters
  # 'permission' is the key of the permission to add.
  #   For global permissions, available values are : admin, profileadmin, shareDashboard, scan, dryRunScan, provisioning.
  #   For component permissions, available values are : user, codeviewer, admin.
  # 'user' is the user identifier (login)
  # OR
  # 'group' is the group identifier (group name or 'anyone')
  #
  # -- Optional parameters
  # 'component' is the component key on which add the permission. If null, the permission should be a global permission.
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/permissions/remove?permission=shareDashboard&user=new_user'
  #
  # -- Notes
  # An exception will be raised if both a user and a group are provided
  # Requests that attempt to remove a non-existing permission will be silently ignored
  #
  # since 3.7
  # 'component' parameter and 'provisioning' permission have been added in 4.0
  #
  def remove
    verify_post_request
    require_parameters :permission, (params[:user].nil? ? :group : :user)
    Internal.permissions.removePermission(params)
    hash = {:user => params[:user], :group => params[:group], :permission => params[:permission]}
    respond_to do |format|
      format.json { render :json => jsonp(hash), :status => 200 }
      format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'sonar', :status => 200) }
    end
  end

end

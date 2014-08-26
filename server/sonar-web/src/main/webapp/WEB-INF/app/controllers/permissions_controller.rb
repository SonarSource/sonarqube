#
# Sonar, entreprise quality control tool.
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
class PermissionsController < ApplicationController

  #
  # GET /permissions/search_users?permission=<permission>&component=<component_key>&selected=all&page=5%pageSize=50&query=john
  #
  # Possible value of 'selected' are 'selected', 'deselected' and 'all' ()
  #
  def search_users
    result = Internal.permissions.findUsersWithPermission(params)
    users = result.users()
    more = result.hasMoreResults()
    respond_to do |format|
      format.json {
        render :json => {
            :more => more,
            :results => users.map { |user| {
                :login => user.login(),
                :name => user.name(),
                :selected => user.hasPermission()
            }}
        }
      }
    end
  end

  #
  # GET /permissions/search_groups?permission=<permission>&component=<component_key>&selected=all&page=5%pageSize=50&query=users
  #
  # Possible value of 'selected' are 'selected', 'deselected' and 'all' ()
  #
  def search_groups
    result = Internal.permissions.findGroupsWithPermission(params)
    groups = result.groups()
    more = result.hasMoreResults()
    respond_to do |format|
      format.json {
        render :json => {
            :more => more,
            :results => groups.map { |group|
              hash = {
                  :name => group.name(),
                  :selected => group.hasPermission()
              }
              hash[:description] = group.description() if group.description() && !group.description().blank?
              hash
            }
        }
      }
    end
  end

end

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
class UserGroupsController < ApplicationController

  #
  # GET /user_groups/search?user=<login>&selected=<selected>&page=3&pageSize=10&query=<query>
  #
  # Possible value of 'selected' are 'selected', 'deselected' and 'all' ()
  #
  def search
    result = Internal.group_membership.find(params)
    groups = result.groups()
    more = result.hasMoreResults()

    respond_to do |format|
      format.json {
        render :json => {
            :more => more,
            :results => groups.map { |group|
              hash = {
                  :id => group.id(),
                  :name => group.name(),
                  :selected => group.isMember()
              }
              hash[:description] = group.description() if group.description() && !group.description().blank?
              hash
            }
        }
      }
    end
  end

end

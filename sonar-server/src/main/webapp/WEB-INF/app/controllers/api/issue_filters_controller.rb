#
# SonarQube, open source software quality management tool.
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

# since 4.2
class Api::IssueFiltersController < Api::ApiController

  # GET /api/issue_filters/favorites/<id>
  def favorites
    if logged_in?
      favorite_filters = Internal.issues.findFavouriteIssueFiltersForCurrentUser()
    else
      favorite_filters = []
    end

    hash = {
      :favoriteFilters => favorite_filters.map do |filter|
        {
          :id => filter.id().to_i,
          :name => filter.name(),
          :user => filter.user(),
          :shared => filter.shared()
          # no need to export description and query fields
        }
      end
    }

    respond_to do |format|
      format.json { render :json => jsonp(hash), :status => 200 }
    end
  end
end

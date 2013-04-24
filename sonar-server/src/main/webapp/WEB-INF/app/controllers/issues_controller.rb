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

class IssuesController < ApplicationController

  def index
    page_index = params[:page_index] || 1
    issues_result = find_issues({'pageSize' => 25, 'pageIndex' => page_index})
    @paging = issues_result.paging
    @issues = issues_result.issues.collect {|issue| issue}
  end

  protected

  def find_issues(map)
    user = current_user ? current_user.id : nil
    Api.issues.find(map, user)
  end

end
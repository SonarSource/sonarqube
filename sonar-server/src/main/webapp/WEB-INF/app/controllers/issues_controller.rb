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

  SECTION=Navigation::SECTION_RESOURCE

  def index
    @issues = find_issues({})
  end

  # Used for the permalink, e.g. http://localhost:9000/issues/view/1
  def view
    issues = find_issues({'keys' => params[:id]})
    if issues.length == 1
      @issue = issues[0]
      @resource = Project.by_key(@issue.component_key)
      render 'issues/_view', :locals => {:issue => @issue}
    else
      render :text => "<b>Cannot access this issue</b> : not found."
    end
  end

  protected

  def find_issues(map)
    user = current_user ? current_user.id : nil
    Api.issues.find(map, user).issues
  end

end
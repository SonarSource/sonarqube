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

class IssueController < ApplicationController

  SECTION=Navigation::SECTION_RESOURCE

  # Used for the permalink, e.g. http://localhost:9000/issue/view/1
  def view
    issue_result = find_issues({'issueKeys' => params[:id]})
    if issue_result.issues.length == 1
      @issue = issue_result.issues[0]
      @rule = issue_result.rule(@issue)
      @resource = Project.by_key(@issue.component_key)
      render 'issue/_view', :locals => {:issue => @issue, :rule => @rule, :resource => @resource}
    else
      render :text => "<b>Cannot access this issue</b> : not found."
    end
  end

  protected

  def find_issues(map)
    Api.issues.find(map)
  end

end
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

  helper SourceHelper, UsersHelper

  # Used for the permalink, e.g. http://localhost:9000/issue/view/1
  def view
    require_parameters :id
    issue_result = find_issue(params[:id])
    @issue = issue_result.issues[0]
    @rule = issue_result.rule(@issue)
    @component = Project.by_key(@issue.component_key)
    @transitions = Internal.issues.listTransitions(@issue.key)

    # Used for breadcrumb
    @resource = @component.root_project
    render 'issue/_view'
  end

  # GET
  def transition_form
    require_parameters :key, :transition
    issue_result = find_issue(params[:key])
    @issue = issue_result.issues[0]
    bad_request('Unknown issue') unless @issue

    @transition = params[:transition]
    bad_request('Missing transition') if @transition.blank?

    render :partial => 'issue/transition_form'
  end

  # POST
  def transition
    verify_post_request
    require_parameters :key, :transition

    issue = Internal.issues.doTransition(params[:key], params[:transition])


    issue_result = find_issue(params[:key])
    @issue = issue_result.issues[0]
    @rule = issue_result.rule(@issue)
    @component = Project.by_key(@issue.component_key)
    @transitions = Internal.issues.listTransitions(@issue.key)
    @resource = @component.root_project

    if issue
      render :partial => 'issue/view'
    else
      render :status => 400
    end
  end

  protected

  def find_issues(map)
    Api.issues.find(map)
  end

  def find_issue(issue_key)
    issue_result = find_issues({'issueKeys' => issue_key})
    if issue_result.issues.length == 1
      issue_result
    else
      render :text => "<b>Cannot access this issue</b> : not found."
    end
  end

end
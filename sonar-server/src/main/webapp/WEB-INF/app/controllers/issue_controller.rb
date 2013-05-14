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

  #
  #
  # ACTIONS FROM ISSUE DETAIL PAGE
  #
  #


  # Used for the permalink, e.g. http://localhost:9000/issue/view/1
  def view
    require_parameters :id
    init_issue(params[:id])
    init_resource
    @transitions = Internal.issues.listTransitions(@issue.key) if current_user
    render 'issue/_view'
  end

  def show
    require_parameters :key
    init_issue(params[:key])
    render_issue_detail
  end

  def transition_form
    require_parameters :issue, :transition

    init_issue(params[:issue])
    @transition = params[:transition]
    bad_request('Missing transition') if @transition.blank?

    render :partial => 'issue/transition_form'
  end

  def transition
    verify_post_request
    require_parameters :issue, :transition

    @issue = Internal.issues.doTransition(params[:issue], params[:transition])
    init_issue(params[:issue])
    render_issue_detail
  end

  def assign_form
    require_parameters :issue
    init_issue(params[:issue])

    render :partial => 'issue/assign_form'
  end

  def assign
    verify_post_request
    require_parameters :issue

    assignee = nil
    if params[:me]=='true'
      assignee = current_user.login

    elsif params[:issue_assignee_login].present?
      assignee = params[:issue_assignee_login]
    end

    @issue = Internal.issues.assign(params[:issue], assignee)
    init_issue(params[:issue])
    render_issue_detail
  end

  def change_severity_form
    require_parameters :issue
    init_issue(params[:issue])

    render :partial => 'issue/change_severity_form'
  end

  def change_severity
    verify_post_request
    require_parameters :issue, :severity

    @issue = Internal.issues.setSeverity(params[:issue], params[:severity])
    init_issue(params[:issue])
    render_issue_detail
  end

  def plan_form
    require_parameters :issue
    init_issue(params[:issue])
    init_resource
    @action_plans = Internal.issues.findOpenActionPlans(@resource.key)

    render :partial => 'issue/plan_form'
  end

  def plan
    verify_post_request
    require_parameters :issue, :plan

    @issue = Internal.issues.plan(params[:issue], params[:plan])
    init_issue(params[:issue])
    render_issue_detail
  end

  def unplan
    verify_post_request
    require_parameters :issue

    @issue = Internal.issues.plan(params[:issue], nil)
    init_issue(params[:issue])
    render_issue_detail
  end

  def action_form
    verify_ajax_request
    require_parameters :id, :issue

    action_type = params[:id]

    # not used yet
    issue_key = params[:issue]

    render :partial => "issue/#{action_type}_form"
  end

  def do_action
    verify_post_request
    require_parameters :id, :issue

    issue_key = params[:issue]
    action_type = params[:id]

    if action_type=='comment'
      Internal.issues.addComment(issue_key, params[:text])
    elsif action_type=='assign'
      assignee = (params[:me]=='true' ? current_user.login : params[:assignee])
      Internal.issues.assign(issue_key, assignee)
    end

    @issue_results = Api.issues.find(issue_key)
    render :partial => 'resource/issue', :locals => {:issue => @issue_results.issues.get(0)}
  end

  #
  #
  # ACTIONS FROM ISSUES TAB OF CODE VIEWER
  #
  #

  def display_issue
    init_issue(params[:issue])
    render :partial => 'resource/issue', :locals => {:issue => @issue}
  end

  def issue_transition_form
    require_parameters :issue, :transition

    init_issue(params[:issue])
    @transition = params[:transition]
    bad_request('Missing transition') if @transition.blank?

    render :partial => 'issue/code_viewer/transition_form'
  end

  def issue_transition
    verify_post_request
    require_parameters :issue, :transition

    @issue = Internal.issues.doTransition(params[:issue], params[:transition])
    init_issue(params[:issue])
    render_issue_code_viewer
  end

  def issue_assign_form
    require_parameters :issue
    init_issue(params[:issue])

    render :partial => 'issue/code_viewer/assign_form'
  end

  def issue_assign
    verify_post_request
    require_parameters :issue

    assignee = nil
    if params[:me]=='true'
      assignee = current_user.login

    elsif params[:issue_assignee_login].present?
      assignee = params[:issue_assignee_login]
    end

    @issue = Internal.issues.assign(params[:issue], assignee)
    init_issue(params[:issue])
    render_issue_code_viewer
  end

  def issue_change_severity_form
    require_parameters :issue
    init_issue(params[:issue])

    render :partial => 'issue/code_viewer/change_severity_form'
  end

  def issue_change_severity
    verify_post_request
    require_parameters :issue, :severity

    @issue = Internal.issues.setSeverity(params[:issue], params[:severity])
    init_issue(params[:issue])
    render_issue_code_viewer
  end

  def issue_plan_form
    require_parameters :issue
    init_issue(params[:issue])
    init_resource
    @action_plans = Internal.issues.findOpenActionPlans(@resource.key)

    render :partial => 'issue/code_viewer/plan_form'
  end

  def issue_plan
    verify_post_request
    require_parameters :issue, :plan

    @issue = Internal.issues.plan(params[:issue], params[:plan])
    init_issue(params[:issue])
    render_issue_code_viewer
  end

  def issue_unplan
    verify_post_request
    require_parameters :issue

    @issue = Internal.issues.plan(params[:issue], nil)
    init_issue(params[:issue])
    render_issue_code_viewer
  end


  #
  #
  # ACTIONS FROM THE ISSUES WIDGETS
  #
  #

  def widget_issues_list
    @dashboard_configuration = Api::DashboardConfiguration.new(nil, :period_index => params[:period])
    render :partial => 'project/widgets/issues/issues_list'
  end


  protected

  def init_issue(issue_key)
    @issue_results = find_issue(issue_key)
    @issue = @issue_results.issues[0]
  end

  def init_resource
    @component = Project.by_key(@issue.component_key)
    @resource = @component.root_project
  end

  def render_issue_detail
    if @issue
      init_resource
      @transitions = Internal.issues.listTransitions(@issue.key) if current_user
      render :partial => 'issue/view'
    else
      # TODO
      render :status => 400
    end
  end

  def render_issue_code_viewer
    if @issue
      @transitions = Internal.issues.listTransitions(@issue.key) if current_user
      render :partial => 'resource/issue', :locals => {:issue => @issue}
    else
      # TODO
      render :status => 400
    end
  end

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
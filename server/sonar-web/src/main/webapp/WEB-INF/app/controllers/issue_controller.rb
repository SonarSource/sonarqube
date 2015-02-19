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

require 'set'

class IssueController < ApplicationController

  def do_action
    verify_ajax_request
    verify_post_request
    require_parameters :id, :issue

    action_type = params[:id]
    issue_key = params[:issue]

    if action_type=='comment'
      issue_result = Internal.issues.addComment(issue_key, params[:text])
    elsif action_type=='assign'
      assignee = (params[:me]=='true' ? current_user.login : params[:assignee])
      issue_result = Internal.issues.assign(issue_key, assignee)
    elsif action_type=='transition'
      issue_result = Internal.issues.doTransition(issue_key, params[:transition])
    elsif action_type=='severity'
      issue_result = Internal.issues.setSeverity(issue_key, params[:severity])
    elsif action_type=='plan'
      issue_result = Internal.issues.plan(issue_key, params[:plan])
    elsif action_type=='unplan'
      issue_result = Internal.issues.plan(issue_key, nil)
    else
      # Execute action defined by plugin
      issue_result = Internal.issues.executeAction(issue_key, action_type)
    end

    if issue_result.ok
      init_issue(issue_key)
      render :partial => 'issue/issue', :locals => {:issue => @issue}
    else
      @errors = issue_result.errors
      render :partial => 'issue/error', :status => issue_result.httpStatus
    end

  end

  # Edit and save an existing comment
  def edit_comment
    verify_ajax_request
    verify_post_request
    require_parameters :key

    text = Api::Utils.read_post_request_param(params[:text])
    edit_result = Internal.issues.editComment(params[:key], text)

    if edit_result.ok
      init_issue(edit_result.get.issueKey)
      render :partial => 'issue/issue', :locals => {:issue => @issue}
    else
      @errors = edit_result.errors
      render :partial => 'issue/error', :status => edit_result.httpStatus
    end
  end

  # Delete an existing comment
  def delete_comment
    verify_post_request
    verify_ajax_request
    require_parameters :id

    comment = Internal.issues.deleteComment(params[:id])

    init_issue(comment.issueKey)
    render :partial => 'issue/issue', :locals => {:issue => @issue}
  end

  # Create a manual issue
  def create
    verify_post_request
    verify_ajax_request

    component_key = params[:component]
    if Api::Utils.is_integer?(component_key)
      component = Project.find(component_key)
      component_key = (component && component.key)
    end

    issue_result = Internal.issues.create(params.merge({:component => component_key}))
    if issue_result.ok
      render :partial => 'issue/manual_issue_created', :locals => {:issue => Internal.issues.getIssueByKey(issue_result.get.key)}
    else
      render :partial => 'shared/result_messages', :status => 500, :locals => {:result => issue_result}
    end
  end

  def show
    # the redirect is needed for the backward compatibility with eclipse plugin
    redirect_to :controller => 'issues', :action => 'search', :anchor => 'issues=' + params[:id]
  end


  #
  #
  # ACTIONS FROM THE ISSUES WIDGETS
  #
  #

  def widget_issues_list
    @snapshot = Snapshot.find(params[:snapshot_id]) if params[:snapshot_id]
    @dashboard_configuration = Api::DashboardConfiguration.new(nil, :period_index => params[:period], :snapshot => @snapshot)
    render :partial => 'project/widgets/issues/issues_list'
  end

  # Display the rule description in the issue panel
  def rule
    verify_ajax_request
    require_parameters :id

    @rule = Internal.rules.findByKey(params[:id])
    if @rule.debtCharacteristicKey()
      @characteristic = Internal.debt.characteristicByKey(@rule.debtCharacteristicKey())
      @sub_characteristic = Internal.debt.characteristicByKey(@rule.debtSubCharacteristicKey())
    end
    render :partial => 'issue/rule'
  end

  # Display the changelog in the issue panel
  def changelog
    verify_ajax_request
    require_parameters :id
    @issue = Internal.issues.getIssueByKey(params[:id])
    @changelog = Internal.issues.changelog(@issue)
    render :partial => 'issue/changelog'
  end


  private

  def init_issue(issue_key)
    @issue = Internal.issues.getIssueByKey(issue_key)
    @project = Internal.component_api.findByUuid(@issue.projectUuid())
    @component = Internal.component_api.findByUuid(@issue.componentUuid())
    @rule = Internal.rules.findByKey(@issue.ruleKey().to_s)
    @action_plan = Internal.issues.findActionPlan(@issue.actionPlanKey()) if @issue.actionPlanKey()
    @comments = Internal.issues.findComments(issue_key)

    user_logins = Set.new
    user_logins << @issue.assignee() if @issue.assignee()
    user_logins << @issue.reporter() if @issue.reporter()
    @comments .each do |comment|
      user_logins << comment.userLogin() if comment.userLogin()
    end
    users = Internal.users_api.find({'logins', user_logins})
    @users = {}
    users.each do |u|
      @users[u.login()] = u
    end

    resource = Project.by_key(@component.key())
    @snapshot = resource.last_snapshot if resource.last_snapshot
  end

end

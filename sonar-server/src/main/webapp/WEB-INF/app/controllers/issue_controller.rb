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

  helper SourceHelper

  # GET /issue/show/<key>
  # This URL is used by the Eclipse Plugin
  #
  # ==== Optional parameters
  # 'layout' is false to remove sidebar and headers. Default value is true.
  # 'source' is false to hide source code. Default value is true.
  #
  # ==== Example
  # GET /issue/show/151f6853-58a1-4950-95e3-9866f8be3e35?layout=false&source=false
  #
  def show
    require_parameters :id
    init_issue

    if params[:modal]
      render :partial => 'issue/show_modal'
    elsif request.xhr?
      if params[:only_detail]
        # used when canceling edition of comment -> see issue.js#refreshIssue()
        render :partial => 'issue/issue', :locals => {:issue => @issue_results.first}
      else
        render :partial => 'issue/show'
      end
    else
      render :action => 'show'
    end

  end

  # Form used for: assign, comment, transition, change severity and plan
  def action_form
    verify_ajax_request
    require_parameters :id, :issue

    @issue_result = Api.issues.find(params[:issue])
    @issue = @issue_result.first

    bad_request('Unknown issue') unless @issue

    action_type = params[:id]
    render :partial => "issue/#{action_type}_form"
  end

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
      @issue_results = Api.issues.find(issue_key)
      render :partial => 'issue/issue', :locals => {:issue => @issue_results.first}
    else
      @errors = issue_result.errors
      render :partial => 'issue/error', :status => issue_result.httpStatus
    end

  end

  # Form used to edit comment
  def edit_comment_form
    verify_ajax_request
    require_parameters :id

    @comment = Internal.issues.findComment(params[:id])

    render :partial => 'issue/edit_comment_form'
  end

  # Edit and save an existing comment
  def edit_comment
    verify_ajax_request
    verify_post_request
    require_parameters :key, :text

    text = Api::Utils.read_post_request_param(params[:text])
    comment = Internal.issues.editComment(params[:key], text)

    @issue_results = Api.issues.find(comment.issueKey)
    render :partial => 'issue/issue', :locals => {:issue => @issue_results.issues.get(0)}
  end

  # Form in a modal window to delete comment
  def delete_comment_form
    verify_ajax_request
    require_parameters :id
    render :partial => 'issue/delete_comment_form'
  end

  # Delete an existing comment
  def delete_comment
    verify_post_request
    verify_ajax_request
    require_parameters :id

    comment = Internal.issues.deleteComment(params[:id])

    @issue_results = Api.issues.find(comment.issueKey)
    render :partial => 'issue/issue', :locals => {:issue => @issue_results.issues.get(0)}
  end

  # Form used to create a manual issue
  def create_form
    verify_ajax_request
    require_parameters :component
    render :partial => 'issue/create_form'
  end

  # Create a manual issue
  def create
    verify_post_request
    verify_ajax_request

    component_key = params[:component]
    if Api::Utils.is_integer?(component_key)
      component = Project.find(component_key)
      component_key = (component ? component.key : nil)
    end

    issue_result = Internal.issues.create(params.merge({:component => component_key}))
    if issue_result.ok
      @issue_results = Api.issues.find(issue_result.get.key)
      render :partial => 'issue/manual_issue_created', :locals => {:issue => @issue_results.first}
    else
      render :partial => 'shared/result_messages', :status => 500, :locals => {:result => issue_result}
    end
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
    rule_key = params[:id].split(':')
    @rule = Rule.first(:conditions => ['plugin_name=? and plugin_rule_key=?', rule_key[0], rule_key[1]], :include => :rule_note)
    render :partial => 'issue/rule'
  end

  # Display the changelog in the issue panel
  def changelog
    verify_ajax_request
    require_parameters :id
    @issue_results = Api.issues.find(params[:id])
    @issue = @issue_results.first()
    @changelog = Internal.issues.changelog(params[:id])
    render :partial => 'issue/changelog'
  end


  private

  def init_issue
    @issue_results = Api.issues.find(params[:id])
    @issue = @issue_results.first()

    resource = Project.by_key(@issue.componentKey())
    @snapshot = resource.last_snapshot if resource.last_snapshot
  end

end
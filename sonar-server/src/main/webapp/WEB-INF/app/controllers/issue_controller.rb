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

  def show
    require_parameters :id

    @issue_results = Api.issues.find(params[:id])
    if request.xhr?
      render :partial => 'issue/issue', :locals => {:issue => @issue_results.issues.get(0)}
    else
      params[:layout] = 'false'
      render :action => 'show'
    end
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
    elsif action_type=='transition'
      Internal.issues.doTransition(issue_key, params[:transition])
    elsif action_type=='severity'
      Internal.issues.setSeverity(issue_key, params[:severity])
    elsif action_type=='plan'
      Internal.issues.plan(issue_key, params[:plan])
    end

    @issue_results = Api.issues.find(issue_key)
    render :partial => 'issue/issue', :locals => {:issue => @issue_results.issues.get(0)}
  end

  # form to edit comment
  def edit_comment_form
    verify_ajax_request
    require_parameters :id

    @comment = Internal.issues.findComment(params[:id])

    render :partial => 'issue/edit_comment_form'
  end

  def edit_comment
    verify_post_request
    require_parameters :key, :text

    text = Api::Utils.read_post_request_param(params[:text])
    comment = Internal.issues.editComment(params[:key], text)

    @issue_results = Api.issues.find(comment.issueKey)
    render :partial => 'issue/issue', :locals => {:issue => @issue_results.issues.get(0)}
  end

  # modal window to delete comment
  def delete_comment_form
    verify_ajax_request
    require_parameters :id

    render :partial => 'issue/delete_comment_form'
  end

  def delete_comment
    verify_post_request
    require_parameters :id

    comment = Internal.issues.deleteComment(params[:id])

    @issue_results = Api.issues.find(comment.issueKey)
    render :partial => 'issue/issue', :locals => {:issue => @issue_results.issues.get(0)}
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


end
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

# since 3.6
class Api::IssuesController < Api::ApiController

  #
  # since 4.1
  #
  # GET /api/issues/changelog?issue=<key>
  #
  # -- Example
  # curl -v -u admin:admin 'http://localhost:9000/api/issues/changelog?issue=9b6f89c0-3347-46f6-a6d1-dd6c761240e0'
  #
  def changelog
    require_parameters :issue
    issue_key = params[:issue]
    changelog = Internal.issues.changelog(issue_key)

    hash = {}
    hash[:changelog] = Issue.changelog_to_hash(changelog)

    respond_to do |format|
      format.json { render :json => jsonp(hash) }
      format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'sonar') }
    end
  end

  #
  # GET /api/issues/transitions?issue=<key>
  #
  # -- Example
  # curl -v -u admin:admin 'http://localhost:9000/api/issues/transitions?issue=9b6f89c0-3347-46f6-a6d1-dd6c761240e0'
  #
  def transitions
    require_parameters :issue
    issue_key = params[:issue]
    transitions = Internal.issues.listTransitions(issue_key)
    render :json => jsonp(
      {
        :transitions => transitions.map { |t| t.key() }
      }
    )
  end

  #
  # POST /api/issues/add_comment?issue=<key>&text=<text>
  #
  # -- Mandatory parameters
  # 'issue' is the key of an existing issue
  # 'text' is the markdown message. It can be set as an URL parameter or as the post request body.
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/issues/add_comment?issue=4a2881e7-825e-4140-a154-01f420c43d11&text=foooo'
  #
  def add_comment
    verify_post_request
    require_parameters :issue, :text

    text = Api::Utils.read_post_request_param(params[:text])
    result = Internal.issues.addComment(params[:issue], text)

    http_status = (result.ok ? 200 : 400)
    hash = result_to_hash(result)
    hash[:comment] = Issue.comment_to_hash(result.get) if result.get

    respond_to do |format|
      # if the request header "Accept" is "*/*", then the default format is the first one (json)
      format.json { render :json => jsonp(hash), :status => result.httpStatus }
      format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'sonar', :status => http_status) }
    end
  end

  #
  # POST /api/issues/delete_comment?key=<key>
  #
  # -- Mandatory parameters
  # 'key' is the comment key
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/issues/delete_comment?key=392160d3-a4f2-4c52-a565-e4542cfa2096'
  #
  def delete_comment
    verify_post_request
    require_parameters :key

    comment = Internal.issues.deleteComment(params[:key])
    render :json => jsonp({:comment => Issue.comment_to_hash(comment)})
  end

  #
  # POST /api/issues/edit_comment?key=<key>&text=<new text>
  #
  # -- Mandatory parameters
  # 'key' is the comment key
  # 'text' is the new value
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/issues/edit_comment?key=392160d3-a4f2-4c52-a565-e4542cfa2096&text=foo'
  #
  def edit_comment
    verify_post_request
    require_parameters :key, :text

    text = Api::Utils.read_post_request_param(params[:text])
    result = Internal.issues.editComment(params[:key], text)
    hash = result_to_hash(result)
    hash[:comment] = Issue.comment_to_hash(result.get) if result.get

    respond_to do |format|
      # if the request header "Accept" is "*/*", then the default format is the first one (json)
      format.json { render :json => jsonp(hash), :status => result.httpStatus }
      format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'sonar', :status => result.httpStatus) }
    end
  end

  #
  # GET /api/issues/actions?issue=<key>
  #
  # -- Example
  # curl -v -u admin:admin 'http://localhost:9000/api/issues/actions?issue=9b6f89c0-3347-46f6-a6d1-dd6c761240e0'
  #
  def actions
    require_parameters :issue
    issue_key = params[:issue]
    render :json => jsonp(
      {
        :actions => Internal.issues.listActions(issue_key)
      }
    )
  end

  #
  # Execute a bulk change on a list of issues
  #
  # POST /api/issues/bulk_change?issue=<key>&text=<text>
  #
  # -- Mandatory parameters
  # 'issues' is the list of issue keys
  # 'actions' the list of action to execute (expected at least one). Available actions are : assign,set_severity,plan,do_transition,add_tags
  # Action plans are dropped in 5.5, the 'plan' action has no effect.
  #
  # -- Optional parameters
  # 'assign.assignee' to assign all issues to a user or un-assign.
  # 'set_severity.severity' to change the severity of all issues.
  # Action Plan are dropped in 5.5. 'plan.plan' has no effect. It was to plan all issues to an action plan or unlink.
  # 'do_transition.transition' to execute a transition on all issues.
  # 'add_tags.tags' to add tags on all issues.
  # 'remove_tags.tags' to remove tags on all issues.
  # 'comment' to add a comment on all issues.
  # 'sendNotifications' to send notification for each modified issue (default is 'false')
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/issues/bulk_change?issues=4a2881e7-825e-4140-a154-01f420c43d11,4a2881e7-825e-4140-a154-01f420c43d30&actions=assign,plan,add_tags&assign.assignee=simon&plan.plan=3.7&add_tags.tags=design,convention'
  #
  def bulk_change
    verify_post_request

    comment = Api::Utils.read_post_request_param(params[:comment])
    sendNotifications = params[:sendNotifications] || 'false'
    result = Internal.issues.bulkChange(params, comment, sendNotifications == 'true')
    hash = {}
    hash[:issuesChanged] = {
      :total => result.issuesChanged().size,
    }
    hash[:issuesNotChanged] = {
      :total => result.issuesNotChanged().size,
      :issues => result.issuesNotChanged().map { |issue| issue.key() }
    }

    respond_to do |format|
      # if the request header "Accept" is "*/*", then the default format is the first one (json)
      format.json { render :json => jsonp(hash), :status => 200 }
      format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'sonar', :status => 200) }
    end
  end

  protected


  def result_to_hash(result)
    hash = {}
    if result.errors and !result.errors.empty?
      hash[:errors] = result.errors().map do |error|
        {
          :msg => (error.text ? error.text : Api::Utils.message(error.l10nKey, :params => error.l10nParams))
        }
      end
    end
    hash
  end

end

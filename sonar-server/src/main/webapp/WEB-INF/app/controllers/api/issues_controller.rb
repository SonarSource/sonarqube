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

# since 3.6
class Api::IssuesController < Api::ApiController

  #
  # GET /api/issues/search?<parameters>
  #
  # -- Example
  # curl -v -u admin:admin 'http://localhost:9000/api/issues/search?statuses=OPEN,RESOLVED'
  #
  def search
    results = Api.issues.find(params)
    hash = {
      :maxResultsReached => results.maxResultsReached,
      :paging => paging_to_hash(results.paging),
      :issues => results.issues.map { |issue| Issue.to_hash(issue) },
      :components => results.components.map { |component| component_to_hash(component) },
      :projects => results.projects.map { |project| component_to_hash(project) },
      :rules => results.rules.map { |rule| Rule.to_hash(rule) },
      :users => results.users.map { |user| User.to_hash(user) }
    }
    hash[:actionPlans] = results.actionPlans.map { |plan| ActionPlan.to_hash(plan) } if results.actionPlans.size>0

    respond_to do |format|
      format.json { render :json => jsonp(hash) }
      format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'issues') }
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
  # POST /api/issues/do_transition?issue=<key>&transition=<key>&comment=<optional comment>
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/issues/do_transition?issue=9b6f89c0-3347-46f6-a6d1-dd6c761240e0&transition=resolve'
  #
  def do_transition
    verify_post_request
    require_parameters :issue, :transition

    result = Internal.issues.doTransition(params[:issue], params[:transition])
    render_result_issue(result)
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
    comment = Internal.issues.editComment(params[:issue], text)
    render :json => jsonp({:comment => Issue.comment_to_hash(comment)})
  end

  #
  # Assign an existing issue to a user or un-assign.
  #
  # POST /api/issues/assign?issue=<key>&assignee=<optional assignee>
  # A nil or blank assignee removes the assignee.
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/issues/assign?issue=4a2881e7-825e-4140-a154-01f420c43d11&assignee=emmerik'
  #
  def assign
    verify_post_request
    require_parameters :issue

    result = Internal.issues.assign(params[:issue], params[:assignee])
    render_result_issue(result)
  end


  #
  # Change the severity of an existing issue
  #
  # POST /api/issues/set_severity?issue=<key>&severity=<severity>
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/issues/set_severity?issue=4a2881e7-825e-4140-a154-01f420c43d11&severity=BLOCKER'
  #
  def set_severity
    verify_post_request
    require_parameters :issue, :severity

    result = Internal.issues.setSeverity(params[:issue], params[:severity])
    render_result_issue(result)
  end

  #
  # Link an existing issue to an action plan or unlink
  #
  # POST /api/issues/plan?issue=<key>&plan=<optional plan>
  # A nil or blank plan removes the action plan.
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/issues/plan?issue=4a2881e7-825e-4140-a154-01f420c43d11&plan=6b851f3c-e25c-432c-aee0-0e13a4184ca3'
  #
  def plan
    verify_post_request
    require_parameters :issue

    plan = nil
    plan = params[:plan] if params[:plan] && !params[:plan].blank?
    result = Internal.issues.plan(params[:issue], plan)
    render_result_issue(result)
  end

  #
  # Create a manual issue.
  #
  # POST /api/issues/create
  #
  # -- Mandatory parameters
  # 'component' is the component key
  # 'rule' is the rule key prefixed with "manual:", for example 'manual:performance'
  #
  # -- Optional parameters
  # 'severity' is in BLOCKER, CRITICAL, ... INFO. Default value is MAJOR.
  # 'line' starts at 1
  # 'message' is the plain-text message
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/issues/create?component=commons-io:commons-io:org.apache.commons.io.filefilter.OrFileFilter&rule=manual:performance&line=2&severity=BLOCKER'
  #
  def create
    verify_post_request

    issue_result = Internal.issues.create(params)
    render_result_issue(issue_result)
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
    actions = Internal.issues.listActions(issue_key)
    render :json => jsonp(
        {
            :actions => actions.map { |t| t.key() }
        }
    )
  end


  #
  # POST /api/issues/do_action?issue=<key>&actionKey=<action key>
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/issues/do_action?issue=9b6f89c0-3347-46f6-a6d1-dd6c761240e0&actionKey=link-to-jira'
  #
  def do_action
    verify_post_request
    require_parameters :issue, :actionKey

    result = Internal.issues.executeAction(params[:issue], params[:actionKey])
    render_result_issue(result)
  end

  #
  # Execute a bulk change on a list of issues
  #
  # POST /api/issues/bulk_change?issue=<key>&text=<text>
  #
  # -- Mandatory parameters
  # 'issues' is the list of issue keys
  # 'actions' the list of action to execute (expected at least one). Available actions are : assign,set_severity,plan,do_transition
  #
  # -- Optional parameters
  # 'assign.assignee' to assign all issues to a user or un-assign.
  # 'set_severity.severity' to change the severity of all issues.
  # 'plan.plan' to plan all issues to an action plan or unlink.
  # 'do_transition.transition' to execute a transition on all issues.
  # 'comment' to add a comment on all issues.
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/issues/bulk_change?issues=4a2881e7-825e-4140-a154-01f420c43d11,4a2881e7-825e-4140-a154-01f420c43d30&actions=assign,plan&assign.assignee=simon&plan.plan=3.7'
  #
  def bulk_change
    verify_post_request

    comment = Api::Utils.read_post_request_param(params[:comment])
    result = Internal.issues.bulkChange(params, comment)
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

  def render_result_issue(result)
    hash = result_to_hash(result)
    hash[:issue] = Issue.to_hash(result.get) if result.get

    respond_to do |format|
      # if the request header "Accept" is "*/*", then the default format is the first one (json)
      format.json { render :json => jsonp(hash), :status => result.httpStatus }
      format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'sonar', :status => (result.ok ? 200 : 400)) }
    end
  end

  def component_to_hash(component)
    hash = {
      :key => component.key,
      :qualifier => component.qualifier
    }
    hash[:name] = component.name if component.name
    hash[:longName] = component.longName if component.longName
    hash
  end

  def paging_to_hash(paging)
    {
      :pageIndex => paging.pageIndex,
      :pageSize => paging.pageSize,
      :total => paging.total,
      :pages => paging.pages
    }
  end

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

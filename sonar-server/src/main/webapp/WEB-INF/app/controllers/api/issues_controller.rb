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
      :securityExclusions => results.securityExclusions,
      :paging => paging_to_hash(results.paging),
      :issues => results.issues.map { |issue| issue_to_hash(issue) },
      :rules => results.rules.map { |rule| rule_to_hash(rule) }
    }
    hash[:actionPlans] = results.actionPlans.map { |plan| action_plan_to_hash(plan) } if results.actionPlans.size>0

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
    # TODO deal with errors (404, ...)
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

    issue = Internal.issues.doTransition(params[:issue], params[:transition])
    if issue
      render :json => jsonp({
                              :issue => issue_to_hash(issue)
                            })
    else
      render :status => 400
    end
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
    comment = Internal.issues.addComment(params[:issue], text)
    render :json => jsonp({:comment => comment_to_hash(comment)})
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
    render :json => jsonp({:comment => comment_to_hash(comment)})
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
    render :json => jsonp({:comment => comment_to_hash(comment)})
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

    issue = Internal.issues.assign(params[:issue], params[:assignee])
    render :json => jsonp({:issue => issue_to_hash(issue)})
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

    issue = Internal.issues.setSeverity(params[:issue], params[:severity])

    render :json => jsonp({:issue => issue_to_hash(issue)})
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
    issue = Internal.issues.plan(params[:issue], plan)

    render :json => jsonp({:issue => issue_to_hash(issue)})
  end

  #
  # Create a manual issue.
  #
  # POST /api/issues/create
  #
  # -- Mandatory parameters
  # 'component' is the component key
  # 'rule' includes the repository key and the rule key, for example 'squid:AvoidCycle'
  #
  # -- Optional parameters
  # 'severity' is in BLOCKER, CRITICAL, ... INFO. Default value is MAJOR.
  # 'line' starts at 1
  # 'description' is the plain-text description
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/issues/create?component=commons-io:commons-io:org.apache.commons.io.filefilter.OrFileFilter&rule=pmd:ConstructorCallsOverridableMethod&line=2&severity=BLOCKER'
  #
  def create
    verify_post_request
    access_denied unless logged_in?
    require_parameters :component, :rule

    issue = Internal.issues.create(params)
    render :json => jsonp({:issue => issue_to_hash(issue)})
  end

  private

  def issue_to_hash(issue)
    hash = {
      :key => issue.key,
      :component => issue.componentKey,
      :rule => issue.ruleKey.toString(),
      :status => issue.status
    }
    hash[:actionPlan] = issue.actionPlanKey if issue.actionPlanKey
    hash[:resolution] = issue.resolution if issue.resolution
    hash[:severity] = issue.severity if issue.severity
    hash[:desc] = issue.description if issue.description
    hash[:line] = issue.line.to_i if issue.line
    hash[:effortToFix] = issue.effortToFix.to_f if issue.effortToFix
    hash[:userLogin] = issue.userLogin if issue.userLogin
    hash[:assignee] = issue.assignee if issue.assignee
    hash[:creationDate] = Api::Utils.format_datetime(issue.creationDate) if issue.creationDate
    hash[:updateDate] = Api::Utils.format_datetime(issue.updateDate) if issue.updateDate
    hash[:closeDate] = Api::Utils.format_datetime(issue.closeDate) if issue.closeDate
    hash[:attr] = issue.attributes.to_hash unless issue.attributes.isEmpty()
    hash[:manual] = issue.manual if issue.manual
    if issue.comments.size>0
      hash[:comments] = issue.comments.map { |c| comment_to_hash(c) }
    end
    hash
  end

  def comment_to_hash(comment)
    {
      :key => comment.key(),
      :login => comment.userLogin(),
      :htmlText => Internal.text.markdownToHtml(comment.markdownText()),
      :createdAt => Api::Utils.format_datetime(comment.createdAt())
    }
  end

  def diffs_to_hash(diffs)
    hash = {
      :login => diffs.userLogin(),
      :at => format_datetime(diffs.createdAt())
    }
    hash
  end

  def rule_to_hash(rule)
    l10n_name = Internal.rules.ruleL10nName(rule)
    l10n_desc = Internal.rules.ruleL10nDescription(rule)
    hash = {:key => rule.ruleKey().toString()}
    hash[:name] = l10n_name if l10n_name
    hash[:desc] = l10n_desc if l10n_desc
    hash
  end

  def action_plan_to_hash(action_plan)
    hash = {:key => action_plan.key(), :name => action_plan.name(), :status => action_plan.status()}
    hash[:desc] = action_plan.description() if action_plan.description() && !action_plan.description().blank?
    hash[:userLogin] = action_plan.userLogin() if action_plan.userLogin()
    hash[:deadLine] = Api::Utils.format_datetime(action_plan.deadLine()) if action_plan.deadLine()
    hash[:creationDate] = Api::Utils.format_datetime(action_plan.creationDate()) if action_plan.creationDate()
    hash[:updateDate] = Api::Utils.format_datetime(action_plan.updateDate()) if action_plan.updateDate()
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
end

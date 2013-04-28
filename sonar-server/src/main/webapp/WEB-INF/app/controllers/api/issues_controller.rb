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

  # GET /api/issues/search?<parameters>
  def search
    results = Api.issues.find(params)
    render :json => jsonp(
      {
        :securityExclusions => results.securityExclusions,
        :paging => paging_to_json(results.paging),
        :issues => results.issues.map { |issue| issue_to_json(issue) }
      }
    )
  end

  # GET /api/issues/transitions?issue=<key>
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

  # POST /api/issues/do_transition?issue=<key>&transition=<key>&comment=<optional comment>
  def do_transition
    verify_post_request
    require_parameters :issue, :transition
    #access_denied unless logged_in?

    issue = Internal.issues.doTransition(params[:issue], params[:transition])
    if issue
      render :json => jsonp({
        :issue => issue_to_json(issue)
      })
    else
      render :status => 400
    end
  end

  # POST /api/issues/create?severity=xxx>&<resolution=xxx>&component=<component key>
  def create
    verify_post_request
    access_denied unless logged_in?

    # TODO
    render :json => jsonp({})
  end

  private

  def issue_to_json(issue)
    json = {
      :key => issue.key,
      :component => issue.componentKey,
      :rule => issue.ruleKey.toString(),
      :resolution => issue.resolution,
      :status => issue.status
    }
    json[:severity] = issue.severity if issue.severity
    json[:desc] = issue.description if issue.description
    json[:line] = issue.line if issue.line
    json[:cost] = issue.cost if issue.cost
    json[:userLogin] = issue.userLogin if issue.userLogin
    json[:assignee] = issue.assignee if issue.assignee
    json[:createdAt] = format_java_datetime(issue.createdAt) if issue.createdAt
    json[:updatedAt] = format_java_datetime(issue.updatedAt) if issue.updatedAt
    json[:closedAt] = format_java_datetime(issue.closedAt) if issue.closedAt
    json[:attr] = issue.attributes.to_hash unless issue.attributes.isEmpty()
    json
  end

  def paging_to_json(paging)
    {
      :pageIndex => paging.pageIndex,
      :pageSize => paging.pageSize,
      :total => paging.total,
      :pages => paging.pages
    }
  end

  def format_java_datetime(java_date)
    java_date ? Api::Utils.format_datetime(Time.at(java_date.time/1000)) : nil
  end

end

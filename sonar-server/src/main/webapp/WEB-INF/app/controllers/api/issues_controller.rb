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
    user_id = current_user ? current_user.id : nil
    results = Api.issues.find(params, user_id)
    render :json => jsonp(results_to_json(results))
  end

  # POST /api/issues/change?key=<issue key>&<newSeverity=xxx>&<newResolution=xxx>...
  def change
    verify_post_request
    access_denied unless logged_in?

    # TODO
    render :json => jsonp({})
  end

  # POST /api/issues/create?severity=xxx>&<resolution=xxx>&component=<component key>
  def create
    verify_post_request
    access_denied unless logged_in?

    # TODO
    render :json => jsonp({})
  end

  private

  def results_to_json(results)
    json = {}
    json[:issues] = results.issues.map { |issue| issue_to_json(issue) }
    json
  end

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

  def format_java_datetime(java_date)
    java_date ? Api::Utils.format_datetime(Time.at(java_date.time/1000)) : nil
  end

end

#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#

class Api::IssuesController < Api::ApiController

  # GET /api/issues/search?<parameters>
  def search
    results = Api.issues.find(params, current_user.id)
    render :json => jsonp(issues_to_json(results.issues))
  end

  private

  def issues_to_json(issues)
    json = []
    issues.each do |issue|
      json << issue_to_json(issue) if issue
    end
    json
  end

  def issue_to_json(issue)
    json = {
        :key => issue.key,
        :component => issue.componentKey,
        :ruleRepository => issue.ruleRepositoryKey,
        :rule => issue.ruleKey,
    }
    json[:severity] = issue.severity if issue.severity
    json[:title] = issue.title if issue.title
    json[:message] = issue.message if issue.message
    json[:line] = issue.line if issue.line
    json[:cost] = issue.cost if issue.cost
    json[:status] = issue.status if issue.status
    json[:resolution] = issue.resolution if issue.resolution
    json[:userLogin] = issue.userLogin if issue.userLogin
    json[:assigneeLogin] = issue.assigneeLogin if issue.assigneeLogin
    json[:createdAt] = to_date(issue.createdAt) if issue.createdAt
    json[:updatedAt] = to_date(issue.updatedAt) if issue.updatedAt
    json[:closedAt] = to_date(issue.closedAt) if issue.closedAt
    json
  end

  def to_date(java_date)
    java_date ? Api::Utils.format_datetime(Time.at(java_date.time/1000)) : nil
  end

end

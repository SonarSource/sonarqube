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

require "json"

class Api::IssuesController < Api::ApiController

  # GETs should be safe (see http://www.w3.org/2001/tag/doc/whenToUseGet.html)
  verify :method => :put, :only => [ :update ]
  verify :method => :post, :only => [ :create ]
  verify :method => :delete, :only => [ :destroy ]

  before_filter :admin_required, :only => [ :create, :update, :destroy ]

  # GET /api/issues
  def index
    map={}
    map['keys'] = params[:keys]
    map['severities'] = params[:severities]
    map['severityMin'] = params[:severityMin]
    map['status'] = params[:status]
    map['resolutions'] = params[:resolutions]
    map['components'] = params[:components]
    map['authors'] = params[:authors]
    map['assignees'] = params[:assignees]
    map['rules'] = params[:rules]
    map['limit'] = params[:limit]

    respond_to do |format|
      format.json { render :json => jsonp(issues_to_json(find_issues(map))) }
      format.xml { render :xml => xml_not_supported }
    end
  end

  #GET /api/issues/foo
  def show
    respond_to do |format|
      format.json { render :json => jsonp(issues_to_json([find_issue(params[:key])])) }
      format.xml { render :xml => xml_not_supported }
    end
  end


  protected

  def find_issues(map)
    issues_query.execute(map)
  end

  def find_issue(key)
    issues_query.execute(key)
  end

  def issues_query
    java_facade.getIssueFilter()
  end

  def issues_to_json(issues)
    json = []
    issues.each do |issue|
      json << issue_to_json(issue) if issue
    end
    json
  end

  def issue_to_json(issue)
    {
        :key => issue.key,
        :component => issue.componentKey,
        :ruleKey => issue.ruleKey,
        :ruleRepositoryKey => issue.ruleRepositoryKey,
        :severity => issue.severity,
        :title => issue.title,
        :message => issue.message,
        :line => issue.line,
        :cost => issue.cost,
        :status => issue.status,
        :resolution => issue.resolution,
        :userLogin => issue.userLogin,
        :assigneeLogin => issue.assigneeLogin,
        :createdAt => to_date(issue.createdAt),
        :updatedAt => to_date(issue.updatedAt),
        :closedAt => to_date(issue.closedAt),
    }
  end

  def to_date(java_date)
    java_date ? Api::Utils.format_datetime(Time.at(java_date.time/1000)) : nil
  end

end

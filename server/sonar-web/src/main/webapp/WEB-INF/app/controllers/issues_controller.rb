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

class IssuesController < ApplicationController

  SECTION=Navigation::SECTION_ISSUES

  # GET /issues/index
  def index
    redirect_to :action => 'search'
  end

  # GET /issues/search
  def search

  end

  def show
    # the redirect is needed for the backward compatibility with eclipse plugin
    redirect_to :action => 'search', :anchor => 'issues=' + params[:id]
  end

  # GET /issues/bulk_change_form?[&criteria]
  def bulk_change_form
    access_denied unless logged_in?

    issues_query_params = criteria_params.clone.merge({'pageSize' => -1})
    # SONAR-4654 pagination parameters should be remove when loading issues for bulk change
    issues_query_params.delete('pageIndex')
    if params[:id]
      @issues = Internal.issues.execute(params[:id].to_i, issues_query_params).issues()
    else
      @issues = Internal.issues.execute(issues_query_params).issues()
    end

    @projectUuids = Set.new(@issues.map {|issue| issue.projectUuid()})
    @tags = Internal.issues.listTags()

    render :partial => 'issues/bulk_change_form'
  end

  # POST /issues/bulk_change?criteria
  def bulk_change
    verify_post_request
    Internal.issues.bulkChange(params, params[:comment], params[:sendNotifications] == 'true')
    render :text => '', :status => 200
  end


  private

  def criteria_params
    new_params = params.clone
    new_params.delete('controller')
    new_params.delete('action')
    new_params
  end

end

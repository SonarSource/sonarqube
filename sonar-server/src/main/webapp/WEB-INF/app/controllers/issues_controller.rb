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

class IssuesController < ApplicationController

  before_filter :init

  def index
    redirect_to :action => 'search'
  end

  def search
    init_results

    @criteria_params = params.merge({:controller => nil, :action => nil, :search => nil, :widget_id => nil, :edit => nil})
    @criteria_params['pageSize'] = 100
    @issue_query = Internal.issues.toQuery(@criteria_params)
    @issues_result = Internal.issues.execute(@issue_query)
    @paging = @issues_result.paging
    @issues = @issues_result.issues
  end

  private

  def init_results
    @issues_result = nil
    @paging = nil
    @issues = nil
    #criteria['pageSize'] = 100
    self
  end

  def init
    @options_for_statuses = Internal.issues.listStatus().map {|s| [message('issue.status.' + s), s]}
    @options_for_resolutions = Internal.issues.listResolutions().map {|s| [message('issue.resolution.' + s), s]}
  end

end
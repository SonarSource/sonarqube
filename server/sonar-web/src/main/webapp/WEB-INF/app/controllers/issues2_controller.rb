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

class Issues2Controller < ApplicationController

  SECTION=Navigation::SECTION_ISSUES2

  before_filter :init_options
  before_filter :load_fav_filters, :only => [:index, :search, :search2, :filter, :manage, :favourites, :toggle_fav]

  PAGE_SIZE = 100

  # GET /issues/index
  def index
    redirect_to :action => 'search'
  end

  # GET /issues/search
  def search

  end

  private

  def init_options
    @options_for_statuses = Internal.issues.listStatus().map { |s| [message('issue.status.' + s), s] }
    @options_for_resolutions = Internal.issues.listResolutions().map { |s| [message('issue.resolution.' + s), s] }
  end

  def load_fav_filters
    @favourite_filters = Internal.issues.findFavouriteIssueFiltersForCurrentUser() if logged_in?
  end

  def find_filter(id)
    Internal.issues.findIssueFilter(id)
  end

  def criteria_params
    new_params = params.clone
    new_params.delete('controller')
    new_params.delete('action')
    new_params
  end

  def init_params
    params['pageSize'] = PAGE_SIZE unless request.xhr?
  end

  def issues_query_params_sanitized
    Internal.issues.sanitizeFilterQuery(params).to_hash
  end

  def issues_query_params_from_filter(filter)
    Internal.issues.deserializeFilterQuery(filter).to_hash
  end

end

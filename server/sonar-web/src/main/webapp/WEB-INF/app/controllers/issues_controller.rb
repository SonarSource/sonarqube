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

  def show
    # the redirect is needed for the backward compatibility with eclipse plugin
    redirect_to :action => 'search', :anchor => 'issues=' + params[:id]
  end

  # GET /issues/manage
  def manage
    @issues_query = Internal.issues.emptyIssueQuery()
    @filters = Internal.issues.findIssueFiltersForCurrentUser()
    @shared_filters = Internal.issues.findSharedFiltersForCurrentUser()
    @favourite_filter_ids = @favourite_filters.map { |filter| filter.id }
  end

  # GET /issues/save_as_form?[&criteria]
  def save_as_form
    @filter_query_serialized = Internal.issues.serializeFilterQuery(criteria_params)
    render :partial => 'issues/filter_save_as_form'
  end

  # POST /issues/save_as?name=<name>[&parameters]
  def save_as
    verify_post_request
    options = {'name' => params[:name], 'description' => params[:description], 'data' => URI.unescape(params[:data]), 'shared' => params[:shared]=='true'}
    @filter = Internal.issues.createIssueFilter(options)
    render :text => @filter.id.to_s, :status => 200
  end

  # POST /issues/save?id=<id>&[criteria]
  def save
    verify_post_request
    require_parameters :id

    @filter = Internal.issues.updateIssueFilterQuery(params[:id].to_i, criteria_params)
    render :text => @filter.id.to_s, :status => 200
  end

  # GET /issues/edit_form/<filter id>
  def edit_form
    require_parameters :id
    @filter = find_filter(params[:id].to_i)
    render :partial => 'issues/filter_edit_form'
  end

  # POST /issues/edit/<filter id>?name=<name>&description=<description>&shared=<true|false>
  def edit
    verify_post_request

    existing_filter = find_filter(params[:id].to_i)
    options = {'id' => params[:id].to_s, 'name' => params[:name], 'description' => params[:description],
               'data' => existing_filter.data, 'shared' => params[:shared]=='true', 'user' => params[:user]}
    @filter = Internal.issues.updateIssueFilter(options)
    render :text => @filter.id.to_s, :status => 200
  end

  # GET /issues/copy_form/<filter id>
  def copy_form
    require_parameters :id
    @filter = find_filter(params[:id].to_i)
    @filter.setUserLogin(nil)
    @filter.setShared(false)
    render :partial => 'issues/filter_copy_form'
  end

  # POST /issues/copy/<filter id>?name=<copy name>&description=<copy description>
  def copy
    verify_post_request

    options = {'name' => params[:name], 'description' => params[:description], 'shared' => params[:shared]=='true'}
    @filter = Internal.issues.copyIssueFilter(params[:id].to_i, options)
    render :text => @filter.id.to_s, :status => 200
  end

  # POST /issues/delete/<filter id>
  def delete
    verify_post_request
    require_parameters :id
    Internal.issues.deleteIssueFilter(params[:id].to_i)
    redirect_to :action => 'manage'
  end

  # GET /issues/favourites
  def favourites
    verify_ajax_request
    render :partial => 'issues/filter_favourites'
  end

  # POST /issues/toggle_fav/<filter id>
  def toggle_fav
    require_parameters :id
    render :text => Internal.issues.toggleFavouriteIssueFilter(params[:id].to_i), :status => 200
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

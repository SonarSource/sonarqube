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
require 'set'

class IssuesController < ApplicationController

  before_filter :init_options
  before_filter :load_filters, :only => [:index, :search, :filter, :manage, :toggle_fav]

  PAGE_SIZE = 100

  # GET /issues/index
  def index
    redirect_to :action => 'search'
  end

  # GET /issues/search
  def search
    if params[:id]
      @filter = find_filter(params[:id].to_i)
    end
    @first_search = Internal.issues.sanitizeFilterQuery(params).to_hash.empty?
    @criteria_params = criteria_params
    issue_filter_result = Internal.issues.execute(@criteria_params)
    @issue_query = issue_filter_result.query
    @issues_result = issue_filter_result.result
  end

  # Load existing filter
  # GET /issues/filter/<filter id>
  def filter
    require_parameters :id

    @first_search = false
    @unchanged = true

    issue_filter_result = Internal.issues.execute(params[:id].to_i, params)
    @filter = find_filter(params[:id].to_i)
    @criteria_params = Internal.issues.deserializeFilterQuery(@filter).to_hash
    @criteria_params[:id] = @filter.id
    @issue_query = issue_filter_result.query
    @issues_result = issue_filter_result.result

    render :action => 'search'
  end

  # GET /issues/manage
  def manage
    @issue_query = Internal.issues.emptyIssueQuery()
    @filters = Internal.issues.findIssueFiltersForCurrentUser()
    @shared_filters = Internal.issues.findSharedFiltersForCurrentUser()
    @favourite_filter_ids = @favourite_filters.map { |filter| filter.id }
  end

  # GET /issues/save_as_form?[&criteria]
  def save_as_form
    @filter_query_serialized = Internal.issues.serializeFilterQuery(params)
    render :partial => 'issues/filter_save_as_form'
  end

  # POST /issues/save_as?name=<name>[&parameters]
  def save_as
    verify_post_request
    options = {'name' => params[:name], 'description' => params[:description], 'data' => URI.unescape(params[:data]), 'shared' => params[:shared]=='true' }
    filter_result = Internal.issues.createIssueFilter(options)

    if filter_result.ok
      @filter = filter_result.get()
      render :text => @filter.id.to_s, :status => 200
    else
      @errors = filter_result.errors
      render :partial => 'issues/filter_save_as_form', :status => 400
    end
  end

  # POST /issues/save?id=<id>&[criteria]
  def save
    verify_post_request
    require_parameters :id

    filter_result = Internal.issues.updateIssueFilterQuery(params[:id].to_i, params)
    if filter_result.ok
      @filter = filter_result.get()
      redirect_to :action => 'filter', :id => @filter.id.to_s
    else
      @unchanged = true
      @errors = filter_result.errors

      issue_filter_result = Internal.issues.execute(@filter.id, params)
      @issue_query = issue_filter_result.query
      @issues_result = issue_filter_result.result

      render :action => 'search'
    end
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
    filter_result = Internal.issues.updateIssueFilter(options)
    if filter_result.ok
      @filter = filter_result.get()
      render :text => @filter.id.to_s, :status => 200
    else
      @errors = filter_result.errors
      render :partial => 'issues/filter_edit_form', :status => 400
    end
  end

  # GET /issues/copy_form/<filter id>
  def copy_form
    require_parameters :id
    @filter = find_filter(params[:id].to_i)
    render :partial => 'issues/filter_copy_form'
  end

  # POST /issues/copy/<filter id>?name=<copy name>&description=<copy description>
  def copy
    verify_post_request

    options = {'name' => params[:name], 'description' => params[:description], 'shared' => params[:shared]=='true' }
    filter_result = Internal.issues.copyIssueFilter(params[:id].to_i, options)

    if filter_result.ok
      @filter = filter_result.get()
      render :text => @filter.id.to_s, :status => 200
    else
      @errors = filter_result.errors
      render :partial => 'issues/filter_copy_form', :status => 400
    end
  end

  # POST /issues/delete/<filter id>
  def delete
    verify_post_request
    require_parameters :id
    Internal.issues.deleteIssueFilter(params[:id].to_i)
    redirect_to :action => 'manage'
  end

  # POST /issues/toggle_fav/<filter id>
  def toggle_fav
    verify_ajax_request
    require_parameters :id
    result = Internal.issues.toggleFavouriteIssueFilter(params[:id].to_i)
    if result.ok
      render :text => '', :status => 200
    else
      @errors = result.errors
      render :action => 'manage'
    end
  end

  # GET /issues/bulk_change_form?[&criteria]
  def bulk_change_form

    # Load maximum number of issues
    @criteria_params = params
    @criteria_params['pageSize'] = -1
    issue_filter_result = Internal.issues.execute(@criteria_params)
    issue_query = issue_filter_result.query
    issues_result = issue_filter_result.result

    @transitions_by_issues = {}
    issues_result.issues.each do |issue|
      transitions = Internal.issues.listTransitions(issue)
      transitions.each do |transition|
        issues_for_transition = @transitions_by_issues[transition.key] || 0
        issues_for_transition += 1
        @transitions_by_issues[transition.key] = issues_for_transition
      end
    end
    @issues = issues_result.issues.map {|issue| issue.key()}
    @project = issue_query.componentRoots.to_a.first if issue_query.componentRoots and issue_query.componentRoots.size == 1

    render :partial => 'issues/bulk_change_form'
  end

  # POST /issues/bulk_change
  def bulk_change
    verify_post_request
    result = Internal.issues.bulkChange(params, "")
    if result.ok
      render :text => params[:criteria_params], :status => 200
    else
      @errors = result.errors
      render :partial => 'issues/bulk_change_form', :status => 400
    end
  end


  private

  def init_options
    @options_for_statuses = Internal.issues.listStatus().map {|s| [message('issue.status.' + s), s]}
    @options_for_resolutions = Internal.issues.listResolutions().map {|s| [message('issue.resolution.' + s), s]}
  end

  def load_filters
    @favourite_filters = Internal.issues.findFavouriteIssueFiltersForCurrentUser()
  end

  def find_filter(id)
    Internal.issues.findIssueFilter(id)
  end

  def criteria_params
    params['pageSize'] = PAGE_SIZE
    params
  end

end
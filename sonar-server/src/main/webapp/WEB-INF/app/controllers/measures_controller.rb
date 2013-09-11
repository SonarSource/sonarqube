#
# SonarQube, open source software quality management tool.
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
class MeasuresController < ApplicationController

  # GET /measures/index
  def index
    @filter = MeasureFilter.new
    render :action => 'search'
  end

  def search
    if params[:id]
      @filter = find_filter(params[:id])
    else
      @filter = MeasureFilter.new
    end
    @filter.criteria=criteria_params
    @filter.enable_default_display
    @filter.execute(self, :user => current_user)

    if request.xhr?
      render :partial => 'measures/display', :locals => {:filter => @filter, :edit_mode => false, :widget_id => params[:widget_id]}
    end
  end

  # Load existing filter
  # GET /measures/filter/<filter id>
  def filter
    require_parameters :id

    @filter = find_filter(params[:id])
    @filter.load_criteria_from_data
    @filter.enable_default_display

    # criteria can be overridden
    @filter.override_criteria(criteria_params)

    @filter.execute(self, :user => current_user)
    @unchanged = true

    render :action => 'search'
  end

  # GET /measures/save_as_form?[id=<id>][&criteria]
  def save_as_form
    if params[:id].present?
      @filter = find_filter(params[:id])
    else
      @filter = MeasureFilter.new
    end
    @filter.criteria=criteria_params_without_page_id
    @filter.convert_criteria_to_data
    render :partial => 'measures/save_as_form'
  end

  # POST /measures/save_as?[id=<id>]&name=<name>[&parameters]
  def save_as
    verify_post_request
    access_denied unless logged_in?

    add_to_favourites=false
    if params[:id].present?
      @filter = find_filter(params[:id])
    else
      @filter = MeasureFilter.new
      @filter.user_id=current_user.id
      add_to_favourites=true
    end
    @filter.name=params[:name]
    @filter.description=params[:description]
    @filter.shared=(params[:shared]=='true') && has_role?(:shareDashboard)
    @filter.data=URI.unescape(params[:data])
    if @filter.save
      current_user.favourited_measure_filters<<@filter if add_to_favourites
      render :text => @filter.id.to_s, :status => 200
    else
      render_measures_error(@filter)
    end
  end

  # POST /measures/save?id=<id>&[criteria]
  def save
    verify_post_request
    require_parameters :id
    access_denied unless logged_in?

    @filter = find_filter(params[:id])
    @filter.criteria=criteria_params_without_page_id
    @filter.convert_criteria_to_data
    unless @filter.save
      flash[:error]='Error'
    end
    redirect_to :action => 'filter', :id => @filter.id
  end

  # GET /measures/manage
  def manage
    access_denied unless logged_in?
    @filter = MeasureFilter.new
    @shared_filters = MeasureFilter.all(:include => :user,
                                         :conditions => ['shared=? and (user_id is null or user_id<>?)', true, current_user.id])
    Api::Utils.insensitive_sort!(@shared_filters) { |elt| elt.name }
    @fav_filter_ids = current_user.measure_filter_favourites.map { |fav| fav.measure_filter_id }
  end

  # GET /measures/edit_form/<filter id>
  def edit_form
    require_parameters :id
    @filter = find_filter(params[:id])
    render :partial => 'measures/edit_form'
  end

  # POST /measures/edit/<filter id>?name=<name>&description=<description>&shared=<true|false>
  def edit
    verify_post_request
    access_denied unless logged_in?
    require_parameters :id

    @filter = MeasureFilter.find(params[:id])
    access_denied unless @filter.owner?(current_user) || has_role?(:admin)

    @filter.name=params[:name]
    @filter.description=params[:description]
    @filter.shared=(params[:shared]=='true') && has_role?(:shareDashboard)
    if has_role?(:admin) && params[:owner]
      @filter.user = User.find_by_login(params[:owner])
    end

    if @filter.save
      # SONAR-4469
      # If filter become unshared then remove all favorite filters linked to it, expect favorite of filter's owner
      MeasureFilterFavourite.delete_all(['user_id<>? and measure_filter_id=?', @filter.user.id, params[:id]]) if params[:shared]!='true'

      render :text => @filter.id.to_s, :status => 200
    else
      render_measures_error(@filter)
    end
  end

  # GET /measures/copy_form/<filter id>
  def copy_form
    require_parameters :id
    @filter = find_filter(params[:id])
    render :partial => 'measures/copy_form'
  end

  # POST /measures/copy/<filter id>?name=<copy name>&description=<copy description>
  def copy
    verify_post_request
    access_denied unless logged_in?
    require_parameters :id

    source = find_filter(params[:id])
    target = MeasureFilter.new
    target.name=params[:name]
    target.description=params[:description]
    target.user_id=current_user.id
    # Copy of filter should never be shared
    target.shared=false
    target.data=source.data
    if target.save
      current_user.favourited_measure_filters << target
      render :text => target.id.to_s, :status => 200
    else
      render_measures_error(target)
    end
  end

  # POST /measures/delete/<filter id>
  def delete
    verify_post_request
    access_denied unless logged_in?
    require_parameters :id

    @filter = find_filter(params[:id])
    @filter.destroy
    redirect_to :action => 'manage'
  end

  def favourites
    verify_ajax_request
    render :partial => 'measures/favourites'
  end

  # POST /measures/toggle_fav/<filter id>
  def toggle_fav
    access_denied unless logged_in?
    verify_ajax_request
    require_parameters :id

    favourites = MeasureFilterFavourite.all(:conditions => ['user_id=? and measure_filter_id=?', current_user.id, params[:id]])
    if favourites.empty?
      filter = find_filter(params[:id])
      current_user.favourited_measure_filters<<filter if filter.shared || filter.owner?(current_user)
      is_favourite = true
    else
      favourites.each { |fav| fav.delete }
      is_favourite = false
    end

    render :text => is_favourite.to_s, :status => 200
  end


  private

  def find_filter(id)
    filter = MeasureFilter.find(id)
    access_denied unless filter.shared || filter.owner?(current_user)
    filter
  end

  def criteria_params_without_page_id
    params.merge({:controller => nil, :action => nil, :search => nil, :widget_id => nil, :edit => nil})
    params.delete(:page)
    params
  end

  def criteria_params
    params.merge({:controller => nil, :action => nil, :search => nil, :widget_id => nil, :edit => nil})
  end

  def render_measures_error(filter)
    errors = []
    filter.errors.full_messages.each{|msg| errors<<CGI.escapeHTML(msg) + '<br/>'}
    render :text => errors, :status => 400
  end
end

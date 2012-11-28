#
# Sonar, open source software quality management tool.
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
class MeasuresController < ApplicationController

  # GET /measures/index
  def index
    @filter = MeasureFilter.new
    render :action => 'search'
  end

  def search
    if params[:id]
      @filter = MeasureFilter.find(params[:id])
    else
      @filter = MeasureFilter.new
    end
    @filter.criteria=(params)
    @filter.enable_default_display
    @filter.execute(self, :user => current_user)
  end

  # Load existing filter
  # GET /measures/filter/<filter id>
  def filter
    require_parameters :id

    @filter = find_filter(params[:id])
    @filter.load_criteria_from_data
    @filter.enable_default_display
    @filter.execute(self, :user => current_user)
    render :action => 'search'
  end

  def save_form
    if params[:id].present?
      @filter = find_filter(params[:id])
    else
      @filter = MeasureFilter.new
    end
    @filter.criteria=(params)
    @filter.convert_criteria_to_data
    render :partial => 'measures/save_form'
  end

  def save
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
    @filter.shared=(params[:shared]=='true')
    @filter.data=URI.unescape(params[:data])
    if @filter.save
      current_user.favourited_measure_filters<<@filter if add_to_favourites
      render :text => @filter.id.to_s, :status => 200
    else
      render :partial => 'measures/save_form', :status => 400
    end
  end

  # GET /measures/manage
  def manage
    access_denied unless logged_in?
    @shared_filters = MeasureFilter.find(:all,
                                         :include => :user,
                                         :conditions => ['shared=? and user_id<>?', true, current_user.id])
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
    access_denied unless owner?(@filter)
    @filter.name=params[:name]
    @filter.description=params[:description]
    @filter.shared=(params[:shared]=='true')
    if @filter.save
      render :text => @filter.id.to_s, :status => 200
    else
      render :partial => 'measures/edit_form', :status => 400
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

    to_clone = find_filter(params[:id])
    @filter = MeasureFilter.new
    @filter.name = params[:name]
    @filter.description = params[:description]
    @filter.user_id = current_user.id
    @filter.shared = to_clone.shared
    @filter.data = to_clone.data
    @filter.shared = false
    if @filter.save
      render :text => @filter.id.to_s, :status => 200
    else
      render :partial => 'measures/copy_form', :status => 400
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

  # POST /measures/toggle_fav/<filter id>
  def toggle_fav
    access_denied unless logged_in?
    verify_ajax_request
    require_parameters :id

    favourites = MeasureFilterFavourite.find(:all,
                                             :conditions => ['user_id=? and measure_filter_id=?', current_user.id, params[:id]])
    if favourites.empty?
      filter = find_filter(params[:id])
      current_user.favourited_measure_filters<<filter if filter.shared || owner?(filter)
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
    access_denied unless filter.shared || owner?(filter)
    filter
  end

  def owner?(filter)
    current_user && (filter.user_id==current_user.id || (filter.user_id==nil && has_role?(:admin)))
  end

end

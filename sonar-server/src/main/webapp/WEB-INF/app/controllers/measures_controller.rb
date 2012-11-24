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

  SECTION=Navigation::SECTION_HOME

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
    @filter.set_criteria_from_url_params(params)
    @filter.execute(self, :user => current_user)
  end

  # Load existing filter
  def filter
    require_parameters :id

    @filter = find_filter(params[:id])
    @filter.load_criteria_from_data
    @filter.execute(self, :user => current_user)
    render :action => 'search'
  end

  def save_form
    if params[:id].present?
      @filter = find_filter(params[:id])
    else
      @filter = MeasureFilter.new
    end
    @filter.set_criteria_from_url_params(params)
    @filter.convert_criteria_to_data
    render :partial => 'measures/save_form'
  end

  def save
    verify_post_request
    access_denied unless logged_in?

    if params[:id].present?
      @filter = find_filter(params[:id])
    else
      @filter = MeasureFilter.new
      @filter.user_id=current_user.id
    end
    @filter.name=params[:name]
    @filter.description=params[:description]
    @filter.shared=(params[:shared]=='true')
    @filter.data=URI.unescape(params[:data])
    if @filter.save
      render :text => @filter.id.to_s, :status => 200
    else
      render :partial => 'measures/save_form', :status => 400
    end
  end

  # GET /measures/manage
  def manage
    access_denied unless logged_in?
  end

  def edit_form
    require_parameters :id
    @filter = find_filter(params[:id])
    render :partial => 'measures/edit_form'
  end

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

  def delete
    verify_post_request
    access_denied unless logged_in?
    require_parameters :id

    @filter = find_filter(params[:id])
    @filter.destroy
    redirect_to :action => 'manage'
  end

  private
  def find_filter(id)
    filter = MeasureFilter.find(id)
    access_denied unless filter.shared || owner?(filter)
    filter
  end

  def owner?(filter)
    current_user && filter.user_id==current_user.id
  end
end

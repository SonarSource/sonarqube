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
class MeasuresController < ApplicationController

  SECTION=Navigation::SECTION_MEASURES

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

    # SONAR-4997
    # Only list display is now managed
    @filter.set_criteria_value(:display, 'list')
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
    # criteria can be overridden
    @filter.override_criteria(criteria_params)

    # SONAR-4997
    # Only list display is now managed
    @filter.set_criteria_value(:display, 'list')

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
    @filter.shared = false
    @filter.user_id = nil
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
    target.shared=(params[:shared]=='true') && has_role?(:shareDashboard)
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

  #
  # GET /measures/search_filter?<parameters>
  #
  # -- Example
  # curl -v -u admin:admin 'http://localhost:9000/measures/search_filter?filter=123&metrics=ncloc,complexity
  #   &fields=name,longName,date,links,favorite,measureTrend,measureStatus,measureVariation&pageSize=100&page=1&sort=metric:ncloc&asc=true'
  #
  def search_filter
    require_parameters :filter

    fields = (params[:fields].split(',') if params[:fields]) || []
    display_links = fields.include?('links')
    display_variation = fields.include?('measureVariation')
    metrics = params[:metrics].split(',') if params[:metrics]

    filter = find_filter(params[:filter])
    filter.load_criteria_from_data
    filter.override_criteria(criteria_params)
    filter.metrics= params[:metrics].split(',') if metrics
    filter.require_links= display_links
    # Force the display to none in case this value was saved to 'list' in the db
    filter.set_criteria_value('display', 'none')
    filter.execute(self, :user => current_user)

    hash = {}
    components_json = []
    filter.rows.each do |row|
      component = row.snapshot.resource
      component_hash = {}
      component_hash[:key] = component.key
      component_hash[:name] = component.name if fields.include?('name') && component.name
      component_hash[:longName] = component.long_name if fields.include?('longName') && component.long_name
      component_hash[:qualifier] = component.qualifier if component.qualifier
      component_hash[:favorite] = logged_in? && current_user.favourite?(component.id) if fields.include?('favourite')
      component_hash[:date] = Api::Utils.format_datetime(row.snapshot.created_at) if fields.include?('date') && row.snapshot.created_at
      component_hash[:fdate] = human_short_date(row.snapshot.created_at) if fields.include?('date') && row.snapshot.created_at

      if display_links && row.links
        links_hash = {}
        row.links.each do |link|
          links_hash[:name] = link.name if link.name
          links_hash[:type] = link.link_type if link.link_type
          links_hash[:url] = link.href if link.href
        end
        component_hash[:links] = links_hash
      end

      if metrics
        component_hash[:measures] = {}
        row.measures.each do |measure|
          component_hash[:measures][measure.metric.key] = {}
          component_hash[:measures][measure.metric.key][:val] = measure.value if measure.value
          component_hash[:measures][measure.metric.key][:fval] = measure.formatted_value if measure.value
          component_hash[:measures][measure.metric.key][:text] = measure.data if measure.data
          component_hash[:measures][measure.metric.key][:status] = measure.alert_status if fields.include?('measureStatus') && measure.alert_status
          component_hash[:measures][measure.metric.key][:p1] = measure.variation_value_1 if display_variation && measure.variation_value_1
          component_hash[:measures][measure.metric.key][:p2] = measure.variation_value_2 if display_variation && measure.variation_value_2
          component_hash[:measures][measure.metric.key][:p3] = measure.variation_value_3 if display_variation && measure.variation_value_3
          component_hash[:measures][measure.metric.key][:p4] = measure.variation_value_4 if display_variation && measure.variation_value_4
          component_hash[:measures][measure.metric.key][:p5] = measure.variation_value_5 if display_variation && measure.variation_value_5
        end
      end
      components_json << component_hash
    end

    hash[:metrics] = {}
    filter.metrics.each do |metric|
      hash[:metrics][metric.key] = {
        :name => metric.short_name,
        :type => metric.val_type,
        :direction => metric.direction
      }
      hash[:metrics][metric.key][:worstValue] = metric.worst_value if metric.worst_value
      hash[:metrics][metric.key][:bestValue] = metric.best_value if metric.best_value
    end

    hash[:components] = components_json
    hash[:maxResultsReached] = filter.security_exclusions
    hash[:paging] = {}
    hash[:paging][:page] = filter.pagination.page
    hash[:paging][:pages] = filter.pagination.pages
    hash[:paging][:pageSize] = filter.pagination.limit
    hash[:paging][:total] = filter.pagination.count

    respond_to do |format|
      format.json { render :json => hash }
    end
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
    filter.errors.full_messages.each { |msg| errors<<CGI.escapeHTML(msg) + '<br/>' }
    render :text => errors, :status => 400
  end

  def human_short_date(date)
    if Date.today - date.to_date == 0
      date.strftime('%H:%M')
    else
      l(date.to_date)
    end
  end

end

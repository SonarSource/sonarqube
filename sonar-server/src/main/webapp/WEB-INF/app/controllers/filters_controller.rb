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
class FiltersController < ApplicationController
  include FiltersHelper
  helper MetricsHelper
  helper FiltersHelper

  SECTION=Navigation::SECTION_CONFIGURATION

  verify :method => :post, :only => [:create, :delete, :up, :down, :activate, :deactivate, :up_column, :down_column, :add_column, :delete_column, :set_sorted_column, :set_view, :set_columns, :set_page_size], :redirect_to => {:action => :index}
  before_filter :login_required, :except => ['index', 'treemap']

  def manage
    @filters = ::Filter.find(:all, :conditions => ['user_id=? or shared=?', current_user.id, true])
  end

  def new
    @filter=::Filter.new()
    @filter.criteria<<Criterion.new_for_qualifiers(['TRK'])
  end

  def create
    @filter=::Filter.new()
    load_filter_from_params(@filter, params)

    @filter.columns.build(:family => 'name', :order_index => 1, :sort_direction => 'ASC')
    @filter.columns.build(:family => 'metric', :kee => 'ncloc', :order_index => 2, :variation => @filter.period?)
    @filter.columns.build(:family => 'metric', :kee => 'violations_density', :order_index => 3, :variation => @filter.period?)
    @filter.columns.build(:family => 'date', :order_index => 4)

    column_index=0
    @filter.measure_criteria.each do |criterion|
      unless @filter.column(criterion.family, criterion.kee)
        @filter.columns.build(:family => criterion.family, :kee => criterion.kee, :order_index => 3+column_index, :sort_direction => (column_index==0 ? 'ASC' : nil))
        column_index+=1
      end
    end

    if @filter.valid?
      @filter.save

      flash[:notice]='Filter saved'

      if params[:preview]=='true'
        redirect_to :action => 'edit', :id => @filter.id
      else
        redirect_to :action => 'manage', :id => @filter.id
      end
    else
      render :action => 'new'
    end
  end

  def edit
    @filter=::Filter.find(params[:id])
    access_denied unless @filter.authorized_to_edit?(self)

    @filter_context=Filters.execute(@filter, self, params)
    render :action => 'new'
  end

  def update
    @filter=::Filter.find(params[:id])
    access_denied unless @filter.authorized_to_edit?(self)

    load_filter_from_params(@filter, params)

    if @filter.save
      flash[:notice]='Filter updated'
      if params[:preview]=='true'
        redirect_to :action => 'edit', :id => @filter.id
      else
        redirect_to :action => 'manage', :id => @filter.id
      end
    else
      render :action => 'new', :id => @filter.id
    end
  end

  def delete
    filter=::Filter.find(:first, :conditions => {:id => params[:id].to_i, :user_id => current_user.id})
    if filter
      filter.destroy
      flash[:notice]='Filter deleted'
    end
    redirect_to :action => 'manage'
  end

  #---------------------------------------------------------------------
  #
  # COLUMNS
  #
  #---------------------------------------------------------------------
  def delete_column
    column=FilterColumn.find(params[:id])
    filter=column.filter

    access_denied unless filter.authorized_to_edit?(self)

    if column.deletable?
      column.destroy
      redirect_to :action => 'edit', :id => filter.id
    else
      flash[:error]='Unknown column'
      redirect_to :action => 'manage'
    end
  end

  def add_column
    filter=::Filter.find(params[:id])
    access_denied unless filter.authorized_to_edit?(self)
    filter.clean_columns_order() # clean the columns which are badly ordered (see SONAR-1902)

    fields=params[:column].split(',')
    family=fields[0]
    if family=='metric'
      filter.columns.create(:family => fields[0], :kee => Metric.by_id(fields[1]).name, :order_index => filter.columns.size + 1, :variation => params[:column_type]=='variation')
    elsif family.present?
      filter.columns.create(:family => family, :order_index => filter.columns.size + 1)
    end
    redirect_to :action => 'edit', :id => filter.id
  end

  def left_column
    column=FilterColumn.find(params[:id])
    filter=column.filter

    access_denied unless filter.authorized_to_edit?(self)

    filter.clean_columns_order() # clean the columns which are badly ordered (see SONAR-1902)
    target_column=filter.column_by_id(params[:id].to_i)
    if target_column.order_index>1
      target_column.order_index-=1
      target_column.save
      old_left_col=filter.columns[target_column.order_index-1]
      old_left_col.order_index+=1
      old_left_col.save
    end
    redirect_to :action => 'edit', :id => filter.id
  end

  def right_column
    column=FilterColumn.find(params[:id])
    filter=column.filter

    access_denied unless filter.authorized_to_edit?(self)

    filter.clean_columns_order() # clean the columns which are badly ordered (see SONAR-1902)
    target_column=filter.column_by_id(params[:id].to_i)
    if target_column.order_index>=1 && target_column.order_index<filter.columns.size
      target_column.order_index+=1
      target_column.save
      old_right_col=filter.columns[target_column.order_index-1]
      old_right_col.order_index-=1
      old_right_col.save
    end
    redirect_to :action => 'edit', :id => filter.id
  end

  def set_sorted_column
    column=FilterColumn.find(params[:id])
    filter=column.filter

    access_denied unless filter.authorized_to_edit?(self)

    filter.columns.each do |col|
      if col==column
        col.sort_direction=params[:sort]
      else
        col.sort_direction=nil
      end
      col.save!
    end
    redirect_to :action => 'edit', :id => filter.id
  end


  #---------------------------------------------------------------------
  #
  # CUSTOMIZE DISPLAY
  #
  #---------------------------------------------------------------------
  def set_view
    filter=::Filter.find(params[:id])
    access_denied unless filter.authorized_to_edit?(self)

    filter.default_view=params[:view]
    filter.save
    redirect_to :action => :edit, :id => filter.id
  end

  def set_columns
    filter=::Filter.find(params[:id])
    access_denied unless filter.authorized_to_edit?(self)

    filter.columns.clear
    params[:columns].each do |colstring|
      column=::FilterColumn.create_from_string(colstring)
      filter.columns<<column if column
    end
    filter.save
    redirect_to :action => :edit, :id => filter.id
  end

  def set_page_size
    filter=::Filter.find(params[:id])
    access_denied unless filter.authorized_to_edit?(self)

    size=[::Filter::MAX_PAGE_SIZE, params[:size].to_i].min
    size=[::Filter::MIN_PAGE_SIZE, size].max
    filter.page_size=size
    filter.save
    redirect_to :action => :edit, :id => filter.id
  end

  #---------------------------------------------------------------------
  #
  # RESOURCE EXPLORER (POPUP)
  #
  #---------------------------------------------------------------------
  def search_path
    if params[:search].present?
      if params[:search].size<3
        flash[:warning]='Please type at least 3 characters'
        redirect_to :action => :search_path

      else
        @snapshots=Snapshot.find(:all, :include => [:project, {:root_snapshot => :project}, {:parent_snapshot => :project}],
                                 :conditions => ['snapshots.status=? AND snapshots.islast=? AND snapshots.scope=? AND projects.scope=? AND UPPER(projects.long_name) LIKE ?', 'P', true, 'PRJ', 'PRJ', "%#{params[:search].upcase}%"],
                                 :order => 'projects.long_name')
        @snapshots=select_authorized(:user, @snapshots)
        @snapshots.sort! do |s1, s2|
          if s1.qualifier==s2.qualifier
            s1.project.long_name<=>s2.project.long_name
          else
            Resourceable::QUALIFIERS.index(s1.qualifier)<=>Resourceable::QUALIFIERS.index(s2.qualifier)
          end
        end
      end
    end
    params[:layout]='false'
  end


  #---------------------------------------------------------------------
  #
  # TREEMAP
  #
  #---------------------------------------------------------------------
  def treemap
    @filter=::Filter.find(params[:id])
    access_denied unless @filter.authorized_to_execute?(self)

    @size_metric=Metric.by_key(params[:size_metric])
    @color_metric=Metric.by_key(params[:color_metric])

    params[:metric_ids]=[@size_metric, @color_metric]

    @filter.sorted_column=FilterColumn.new('family' => 'metric', :kee => @size_metric.key, :sort_direction => (@size_metric.direction>=0 ? 'ASC' : 'DESC'))

    @filter_context=Filters.execute(@filter, self, params)

    @width=(params[:width]||'800').to_i
    @height=(params[:height]||'500').to_i

    @treemap = Sonar::Treemap.new(@filter.id, @size_metric, @width, @height, {
      :color_metric => @color_metric,
      :period_index => @filter_context.period_index,
      :measures_by_snapshot => @filter_context.measures_by_snapshot
    })


    #@treemap=Sonar::Treemap.new(@filter_context.measures_by_snapshot, @width, @height, @size_metric, @color_metric, treemap_options)
    render :action => "treemap", :layout => false

  end


  #---------------------------------------------------------------------
  #
  # PRIVATE METHODS
  #
  #---------------------------------------------------------------------

  private

  def load_filter_from_params(filter, params)
    filter.name=params[:name]
    filter.shared=(params[:shared].present? && is_admin?)
    filter.favourites=params[:favourites].present?
    filter.resource_id=(params[:path_id].present? ? Project.by_key(params[:path_id]).id : nil)
    filter.user_id=current_user.id
    filter.period_index=params[:period_index].to_i
    filter.criteria=[]
    filter.criteria<<Criterion.new_for_qualifiers(params['qualifiers'])
    filter.criteria<<Criterion.new(:family => 'date', :operator => params['date_operator'], :value => params['date_value']) if params['date_operator'].present?
    filter.criteria<<Criterion.new(:family => 'key', :operator => '=', :text_value => params['key_regexp']) if params['key_regexp'].present?
    filter.criteria<<Criterion.new(:family => 'name', :operator => '=', :text_value => params['name_regexp']) if params['name_regexp'].present?
    filter.criteria<<Criterion.new(:family => 'language', :operator => '=', :text_value => params['languages'].join(',')) if params['languages']

    if params[:criteria]['0']['metric_id'].present?
      filter.criteria<<Criterion.new_for_metric(params[:criteria]['0'])
    end
    if params[:criteria]['1']['metric_id'].present?
      filter.criteria<<Criterion.new_for_metric(params[:criteria]['1'])
    end
    if params[:criteria]['2']['metric_id'].present?
      filter.criteria<<Criterion.new_for_metric(params[:criteria]['2'])
    end
  end
end

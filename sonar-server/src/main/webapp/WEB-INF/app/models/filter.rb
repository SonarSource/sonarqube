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
class Filter < ActiveRecord::Base
  VIEW_LIST='list'
  VIEW_TREEMAP='treemap'
  TREEMAP_PAGE_SIZE=200
  DEFAULT_PAGE_SIZE=50
  MAX_PAGE_SIZE=200
  MIN_PAGE_SIZE=20

  belongs_to :user
  belongs_to :resource, :class_name => 'Project', :foreign_key => 'resource_id'
  has_many :columns, :class_name => 'FilterColumn', :dependent => :destroy, :validate => true, :order => 'order_index'
  has_many :criteria, :class_name => 'Criterion', :dependent => :destroy, :validate => true
  has_many :active_filters, :dependent => :destroy

  validates_length_of :name, :within => 1..100
  validates_uniqueness_of :name, :scope => :user_id, :if => Proc.new { |filter| filter.user_id }
  validates_inclusion_of :default_view, :in => ['list', 'treemap'], :allow_nil => true

  def criterion(family, key=nil)
    criteria.each do |criterion|
      if criterion.family==family && criterion.key==key
        return criterion if ((key.nil? && criterion.key.nil?) || (key && key==criterion.key))
      end
    end
    nil
  end

  def measure_criteria
    @measure_criteria ||=
      begin
        criteria.select { |c| c.on_metric? && c.metric }
      end
  end

  def first_column
    columns.size>0 ? columns[0] : nil
  end

  def last_column
    columns.size>0 ? columns[-1] : nil
  end

  def column(family, key=nil)
    columns.each do |col|
      if col.family==family
        return col if ((key.nil? && col.key.nil?) || (key && key==col.key))
      end
    end
    nil
  end

  def measure_columns
    columns.select { |col| col.metric }
  end

  def sorted_column
    @sorted_column ||=
      begin
        columns.to_a.find { |c| c.sort_direction } || column('name')
      end
  end

  def sorted_column=(col_or_id)
    if col_or_id.is_a?(Fixnum)
      @sorted_column=columns.to_a.find { |c| c.id==col_or_id }
    else
      @sorted_column=col_or_id
    end
  end

  def display_masterproject?
    name=='Projects'
  end

  def display_links?
    column('links')
  end

  def default_view
    read_attribute(:default_view) || VIEW_LIST
  end

  def page_size
    if default_view==VIEW_TREEMAP
      TREEMAP_PAGE_SIZE
    else
      read_attribute(:page_size) || DEFAULT_PAGE_SIZE
    end
  end

  def ajax_loading?
    default_view==VIEW_TREEMAP
  end

  def projects_homepage?
    name=='Projects'
  end

  def advanced_search?
    @advanced_search ||=
      begin
        !(criterion('language').nil?) || favourites || !(criterion('name').nil?) || !(criterion('key').nil?) || !(criterion('date').nil?) || period?
      end
  end

  def period_index=(vi)
    if vi && vi>0
      write_attribute(:period_index, vi)
    else
      write_attribute(:period_index, nil)
    end
  end

  def period?
    period_index && period_index>0
  end

  def column_by_id(col_id)
    columns.each do |col|
      return col if col.id==col_id
    end
    nil
  end

  def clean_columns_order
    columns.each_with_index do |col, index|
      col.order_index=index+1
      col.save
    end
    reload
  end

  def authorized_to_execute?(authenticated_system)
    shared || (user==authenticated_system.current_user)
  end

  def authorized_to_edit?(authenticated_system)
    if authenticated_system.logged_in?
      (user && user==authenticated_system.current_user) || (!user && authenticated_system.is_admin?)
    else
      false
    end
  end

  protected

  def before_validation
    # the name column is mandatory
    if self.column('name').nil?
      self.columns.insert(0, FilterColumn.new(:family => 'name'))
    end

    # one column must be sorted
    sorted_col=self.columns.to_a.find { |c| c.sort_direction }
    unless sorted_col
      column('name').sort_direction='ASC'
    end

    # sanitize orders
    self.columns.each_with_index do |col, index|
      col.order_index=index+1
    end
    true
  end

  def after_save
    self.columns.each do |col|
      col.save
    end
  end

end
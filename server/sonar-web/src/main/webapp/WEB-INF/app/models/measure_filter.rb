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
class MeasureFilter < ActiveRecord::Base

  # Row in the table of results
  class Row
    attr_reader :component, :measures_by_metric, :links, :analysis

    def initialize(component)
      @component = component
      @measures_by_metric = {}
      @links = nil
    end

    def resource
      component
    end

    # For internal use
    def add_measure(measure)
      @measures_by_metric[measure.metric] = measure
    end

    # For internal use
    def set_analysis(analysis)
      @analysis=analysis
    end

    # For internal use
    def add_link(link)
      @links ||= []
      @links << link
    end

    def measure(metric)
      @measures_by_metric[metric]
    end

    def measures
      @measures_by_metric.values
    end
  end

  CRITERIA_SEPARATOR = '|'
  CRITERIA_KEY_VALUE_SEPARATOR = ','

  belongs_to :user
  has_many :measure_filter_favourites, :dependent => :delete_all

  validates_presence_of :name, :message => Api::Utils.message('measure_filter.missing_name')
  validates_length_of :name, :maximum => 100, :message => Api::Utils.message('measure_filter.name_too_long')
  validates_length_of :description, :allow_nil => true, :maximum => 4000

  attr_reader :pagination, :security_exclusions, :base_row, :rows, :display

  def sort_key
    criteria['sort']
  end

  def sort_asc?
    criteria['asc']!='false'
  end

  # array of the metrics to use when loading measures
  def metrics
    @metrics ||= []
  end

  # Set the metrics of the result measures to load. Array of Metric or String.
  def metrics=(array=[])
    @metrics = array.map { |m| m.is_a?(Metric) ? m : Metric.by_key(m) }.compact
  end

  # Enable the loading of result links. False by default
  def require_links=(flag)
    @require_links=flag
  end

  # boolean flag that indicates if project links should be loaded
  def require_links?
    @require_links
  end

  def require_authentication?
    criteria[:onFavourites]=='true'
  end

  def can_be_reassigned_by(user)
    user.has_role?(:admin) && shared
  end

  def criteria(key=nil)
    @criteria ||= HashWithIndifferentAccess.new
    if key
      @criteria[key]
    else
      @criteria
    end
  end

  def criteria=(hash)
    @criteria = HashWithIndifferentAccess.new
    hash.each_pair do |k, v|
      set_criteria_value(k, v)
    end
  end

  def override_criteria(hash)
    @criteria ||= HashWithIndifferentAccess.new
    hash.each_pair do |k, v|
      set_criteria_value(k, v)
    end
  end

  # API used by Displays
  def set_criteria_value(key, value)
    @criteria ||= HashWithIndifferentAccess.new
    if key
      if value!=nil && value!='' && value!=['']
        value = (value.kind_of?(Array) ? value : value.to_s)
        @criteria[key]=value
      else
        @criteria.delete(key)
      end
    end
  end

  # API used by Displays
  def set_criteria_default_value(key, value)
    @criteria ||= HashWithIndifferentAccess.new
    unless @criteria.has_key?(key)
      if key
        if value!=nil && value!='' && value!=['']
          value = (value.kind_of?(Array) ? value : value.to_s)
          @criteria[key]=value
        else
          @criteria.delete(key)
        end
      end
    end
  end

  def load_criteria_from_data
    if self.data
      @criteria = self.data.split(CRITERIA_SEPARATOR).inject(HashWithIndifferentAccess.new) do |h, s|
        k, v=s.split('=')
        if k && v
          # nameSearch can contains comma, in this case we should not split the value
          if k != 'nameSearch'
            # Empty values are removed
            v=v.split(CRITERIA_KEY_VALUE_SEPARATOR).select{|v| !v.empty?} if v.include?(CRITERIA_KEY_VALUE_SEPARATOR)
          end
          h[k]=v
        end
        h
      end
    else
      @criteria = HashWithIndifferentAccess.new
    end
  end

  def convert_criteria_to_data
    string_data = []
    if @criteria
      @criteria.each_pair do |k, v|
        string_value = (v.is_a?(String) ? v : v.join(CRITERIA_KEY_VALUE_SEPARATOR))
        string_data << "#{k}=#{string_value}"
      end
    end
    self.data = string_data.join(CRITERIA_SEPARATOR)
  end

  def enable_default_display
    set_criteria_default_value('display', 'list')
  end

  def base_resource
    if criteria('base')
      Project.first(:conditions => ['kee=? and copy_component_uuid is null and developer_uuid is null', criteria('base')])
    end
  end

  def system?
    user_id == nil
  end

  # ==== Options
  # :user : the authenticated user
  def execute(controller, options={})
    init_results
    init_display(options)
    user = options[:user]
    result = Api::Utils.java_facade.executeMeasureFilter(criteria, (user && user.id))
    if result.error
      errors.add_to_base(Api::Utils.message("measure_filter.error.#{result.error}"))
    else
      rows = result.getRows()
      component_uuids = filter_authorized_component_uuids(rows, controller)
      base_project = filter_authorized_base_project(base_resource, controller)
      load_results(component_uuids, base_project)
    end
    self
  end

  def owner?(user)
    return false if user==nil || user.id==nil
    (self.id==nil) || (self.user_id==user.id) || (self.user_id==nil && user.has_role?(:admin))
  end

  private

  def init_results
    @pagination = nil
    @security_exclusions = nil
    @rows = nil
    @base_row = nil
    self
  end

  def init_display(options)
    @display = MeasureFilterDisplay.create(self, options)
  end

  def filter_authorized_base_project(base_resource, controller)
    # SONAR-4793
    # Verify that user has browse permission on base project
    controller.has_role?(:user, base_resource) ? base_resource : nil
  end

  def filter_authorized_component_uuids(rows, controller)
    project_uuids = rows.map { |row| row.getRootComponentUuid() }.compact.uniq
    authorized_project_uuids = controller.select_authorized(:user, project_uuids)
    component_uuids = rows.map { |row| row.getComponentUuid() if authorized_project_uuids.include?(row.getRootComponentUuid()) }.compact
    @security_exclusions = (component_uuids.size<rows.size)
    @pagination = Api::Pagination.new
    @pagination.per_page=(criteria(:pageSize)||999999).to_i
    @pagination.page=(criteria(:page)||1).to_i
    @pagination.count = component_uuids.size
    component_uuids[@pagination.offset ... (@pagination.offset+@pagination.limit)] || []
  end

  def load_results(component_uuids, base_resource)
    @rows = []
    metric_ids = metrics.map(&:id)

    if !component_uuids.empty?
      rows_by_component_uuid = {}

      components = []
      component_uuids.each_slice(999) do |safe_for_oracle_uuids|
        components.concat(Project.find(:all, :conditions => ['uuid in (?)', safe_for_oracle_uuids]))
      end
      project_uuids = []
      components.each do |component|
        row = Row.new(component)
        rows_by_component_uuid[component.uuid] = row
        project_uuids << component.project_uuid
      end
      project_uuids.uniq!

      analysis_by_project_uuid = Snapshot.all(:conditions => ['component_uuid in (?) and islast=?', project_uuids, true]).inject({}) do |hash, analysis|
        hash[analysis.component_uuid] = analysis
        hash
      end

      components.each do |component|
        analysis = analysis_by_project_uuid[component.project_uuid]
        rows_by_component_uuid[component.uuid].set_analysis(analysis) if analysis
      end

      # @rows must be in the same order as the component uuids
      component_uuids.each do |uuid|
        @rows << rows_by_component_uuid[uuid]
      end

      unless metric_ids.empty?
        measures = []
        component_uuids.each_slice(999) do |safe_for_oracle_uuids|
          measures.concat(ProjectMeasure.all(:include => :analysis, :conditions =>
            ['project_measures.person_id is null and project_measures.component_uuid in (?) and project_measures.metric_id in (?) and snapshots.islast=?', safe_for_oracle_uuids, metric_ids, true]
          ))
        end
        measures.each do |measure|
          row = rows_by_component_uuid[measure.component_uuid]
          row.add_measure(measure)
        end
      end

      if require_links?
        uuids_for_links = components.map { |c| c.uuid if c.scope=='PRJ'}.compact.uniq

        uuids_for_links.each_slice(999) do |safe_for_oracle_uuids|
          ProjectLink.all(:conditions => {:component_uuid => safe_for_oracle_uuids}, :order => 'link_type').each do |link|
            rows_by_component_uuid[link.component_uuid].add_link(link)
          end
        end
      end
    end
    if base_resource
      base_snapshot = base_resource.last_snapshot
      if base_snapshot
        @base_row = Row.new(base_resource)
        unless metric_ids.empty?
          base_measures = ProjectMeasure.all(:include => :analysis, :conditions =>
            ['project_measures.person_id is null and project_measures.component_uuid=? and project_measures.metric_id in (?) and snapshots.islast=?', base_resource.uuid, metric_ids, true]
          )
          base_measures.each do |base_measure|
            @base_row.add_measure(base_measure)
          end
        end
      end
    end
  end

  def validate
    # validate uniqueness of name
    if id
      # update existing filter
      if user_id
        count = MeasureFilter.count('id', :conditions => ['name=? and user_id=? and id<>?', name, user_id, id])
      else
        count = MeasureFilter.count('id', :conditions => ['name=? and user_id is null and id<>?', name, id])
      end
    else
      # new filter
      count = MeasureFilter.count('id', :conditions => ['name=? and user_id=?', name, user_id])
    end
    errors.add_to_base('Name already exists') if count>0

    if shared
      if id
        count = MeasureFilter.count('id', :conditions => ['name=? and shared=? and (user_id is null or user_id<>?) and id<>?', name, true, user_id, id])
      else
        count = MeasureFilter.count('id', :conditions => ['name=? and shared=? and (user_id is null or user_id<>?)', name, true, user_id])
      end
      errors.add_to_base('Other users already share filters with the same name') if count>0

      # Verify filter owner has sharing permission
      if user && !user.has_role?(:shareDashboard)
        errors.add(:user, "cannot own this filter because of insufficient rights")
      end
    elsif system?
      errors.add_to_base("System filters can't be unshared")
    end
  end

end

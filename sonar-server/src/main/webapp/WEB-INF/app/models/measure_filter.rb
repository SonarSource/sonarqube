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
require 'set'
class MeasureFilter < ActiveRecord::Base

  # Row in the table of results
  class Result
    attr_reader :snapshot, :measures_by_metric, :links

    def initialize(snapshot)
      @snapshot = snapshot
      @measures_by_metric = {}
      @links = nil
    end

    def add_measure(measure)
      @measures_by_metric[measure.metric] = measure
    end

    def add_link(link)
      @links ||= []
      @links << link
    end

    def measure(metric)
      @measures_by_metric[metric]
    end
  end

  # Column to be displayed
  class Column
    attr_reader :key, :metric

    def initialize(key)
      @key = key
      metric_key = @key.split(':')[1]
      @metric = Metric.by_key(metric_key) if metric_key
    end

    def name
      if @metric
        Api::Utils.message("metric.#{@metric.key}.name", :default => @metric.short_name)
      else
        Api::Utils.message("filters.col.#{@key}", :default => @key)
      end
    end

    def align
      @align ||=
          begin
            # by default is table cells are left-aligned
            (@key=='name' || @key=='short_name' || @key=='description') ? '' : 'right'
          end
    end

    def sort?
      !links?
    end

    def links?
      @key == 'links'
    end
  end

  class Display
    attr_reader :metric_ids

    def initialize(filter)
    end

    def load_links?
      false
    end
  end

  class ListDisplay < Display
    attr_reader :columns

    KEY = :list

    def initialize(filter)
      filter.set_criteria_default_value('columns', ['name', 'short_name', 'description', 'links', 'date', 'language', 'version', 'alert', 'metric:ncloc', 'metric:violations'])
      filter.set_criteria_default_value('sort', 'name')
      filter.set_criteria_default_value('asc', 'true')
      filter.set_criteria_default_value('pageSize', '30')
      filter.pagination.per_page = [filter.criteria['pageSize'].to_i, 200].min
      filter.pagination.page = (filter.criteria['page'] || 1).to_i

      @columns = filter.criteria['columns'].map { |column_key| Column.new(column_key) }
      @metric_ids = @columns.map { |column| column.metric.id if column.metric }.compact.uniq
    end

    def load_links?
      @columns.index { |column| column.links? }
    end
  end

  class TreemapDisplay < Display
    KEY = :treemap

    def initialize(filter)
      filter.set_criteria_default_value('columns', ['metric:ncloc', 'metric:violations'])
      @metric_ids = @columns.map { |column| column.metric.id if column.metric }.compact.uniq
    end
  end

  DISPLAYS = [ListDisplay, TreemapDisplay]

  SUPPORTED_CRITERIA_KEYS=Set.new([:qualifiers, :scopes, :onFavourites, :base, :onBaseComponents, :languages, :fromDate, :toDate, :beforeDays, :afterDays,
                                   :keyRegexp, :nameRegexp,
                                   :sort, :asc, :columns, :display, :pageSize, :page])
  CRITERIA_SEPARATOR = '|'
  CRITERIA_KEY_VALUE_SEPARATOR = ','

# Configuration available after call to execute()
  attr_reader :pagination, :security_exclusions, :columns

# Results : sorted array of Result
  attr_reader :results

  belongs_to :user
  validates_presence_of :name, :message => Api::Utils.message('measure_filter.missing_name')
  validates_length_of :name, :maximum => 100, :message => Api::Utils.message('measure_filter.name_too_long')
  validates_length_of :description, :allow_nil => true, :maximum => 4000

  def criteria
    @criteria ||= {}
  end

  def sort_key
    criteria['sort']
  end

  def sort_asc?
    criteria['asc']=='true'
  end

# API for plugins
  def self.register_display(display_class)
    DISPLAYS<<display_class
  end

  def self.supported_criteria?(key)
    SUPPORTED_CRITERIA_KEYS.include?(key.to_sym)
  end

  def set_criteria_from_url_params(params)
    @criteria = {}
    params.each_pair do |k, v|
      if MeasureFilter.supported_criteria?(k) && !v.empty? && v!=['']
        @criteria[k.to_s]=v
      end
    end
  end

  def load_criteria_from_data
    if self.data
      @criteria = self.data.split(CRITERIA_SEPARATOR).inject({}) do |h, s|
        k, v=s.split('=')
        if k && v
          v=v.split(CRITERIA_KEY_VALUE_SEPARATOR) if v.include?(CRITERIA_KEY_VALUE_SEPARATOR)
          h[k]=v
        end
        h
      end
    else
      @criteria = {}
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

  def display
    @display ||=
        begin
          display_class = nil
          key = criteria['display']
          if key.present?
            display_class = DISPLAYS.find { |d| d::KEY==key.to_sym }
          end
          display_class ||= DISPLAYS.first
          display_class.new(self)
        end
  end


  # ==== Options
  # :user : the authenticated user
  def execute(controller, options={})
    init_results

    user = options[:user]
    rows=Api::Utils.java_facade.executeMeasureFilter2(criteria, (user ? user.id : nil))
    snapshot_ids = filter_authorized_snapshot_ids(rows, controller)
    load_results(snapshot_ids)

    self
  end

# API used by Displays
  def set_criteria_value(key, value)
    if value
      @criteria[key.to_s]=value
    else
      @criteria.delete(key)
    end
  end

# API used by Displays
  def set_criteria_default_value(key, value)
    set_criteria_value(key, value) unless criteria.has_key?(key)
  end

  def url_params
    criteria.merge({'id' => self.id})
  end

  private

  def init_results
    @pagination = Api::Pagination.new
    @security_exclusions = nil
    @results = nil
    self
  end

  def filter_authorized_snapshot_ids(rows, controller)
    project_ids = rows.map { |row| row.getResourceRootId() }.compact.uniq
    authorized_project_ids = controller.select_authorized(:user, project_ids)
    snapshot_ids = rows.map { |row| row.getSnapshotId() if authorized_project_ids.include?(row.getResourceRootId()) }.compact
    @security_exclusions = (snapshot_ids.size<rows.size)
    @pagination.count = snapshot_ids.size
    snapshot_ids[@pagination.offset .. (@pagination.offset+@pagination.limit)]
  end

  def load_results(snapshot_ids)
    @results = []
    if !snapshot_ids.empty?
      results_by_snapshot_id = {}
      snapshots = Snapshot.find(:all, :include => ['project'], :conditions => ['id in (?)', snapshot_ids])
      snapshots.each do |snapshot|
        result = Result.new(snapshot)
        results_by_snapshot_id[snapshot.id] = result
      end

      # @results must be in the same order than the snapshot ids
      snapshot_ids.each do |sid|
        @results << results_by_snapshot_id[sid]
      end

      if display.metric_ids && !display.metric_ids.empty?
        measures = ProjectMeasure.find(:all, :conditions =>
            ['rule_priority is null and rule_id is null and characteristic_id is null and person_id is null and snapshot_id in (?) and metric_id in (?)', snapshot_ids, display.metric_ids]
        )
        measures.each do |measure|
          result = results_by_snapshot_id[measure.snapshot_id]
          result.add_measure measure
        end
      end

      if display.load_links?
        project_ids = []
        results_by_project_id = {}
        snapshots.each do |snapshot|
          project_ids << snapshot.project_id
          results_by_project_id[snapshot.project_id] = results_by_snapshot_id[snapshot.id]
        end
        links = ProjectLink.find(:all, :conditions => {:project_id => project_ids}, :order => 'link_type')
        links.each do |link|
          results_by_project_id[link.project_id].add_link(link)
        end
      end
    end
  end

  def validate
    # validate uniqueness of name
    if id
      # update existing filter
      count = MeasureFilter.count('id', :conditions => ['name=? and user_id=? and id<>?', name, user_id, id])
    else
      # new filter
      count = MeasureFilter.count('id', :conditions => ['name=? and user_id=?', name, user_id])
    end
    errors.add_to_base('Name already exists') if count>0

    if shared
      count = MeasureFilter.count('id', :conditions => ['name=? and shared=? and user_id!=?', name, true, user_id])
      errors.add_to_base('Other users already shared filters with the same name') if count>0
    end
  end
end
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
class MeasureFilter
  # Row in the table of results
  class Data
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
    attr_reader :key

    def initialize(key)
      @key = key
    end

    def metric
      @metric ||=
        begin
          metric_key = @key.split(':')[1]
          Metric.by_key(metric_key) if metric_key
        end
    end

    def display_name
      if metric
        Api::Utils.message("metric.#{metric.key}.name", :default => metric.short_name)
      else
        Api::Utils.message("filters.col.#{@key}", :default => @key)
      end
    end

    def align
      (@key=='name') ? 'left' : 'right'
    end

    def sort?
      !links?
    end

    def links?
      @key == 'links'
    end
  end

  class Display
    def prepare_filter(filter, options)
    end
  end

  class ListDisplay < Display
    def key
      'list'
    end

    def prepare_filter(filter, options)
      filter.set_criteria_default_value(:columns, 'name,date,metric:ncloc,metric:violations')
      filter.set_criteria_default_value(:sort, 'name')
      filter.set_criteria_default_value(:asc, true)
      filter.set_criteria_default_value(:listPageSize, 30)
      filter.pagination.per_page = [filter.criteria[:listPageSize].to_i, 200].min
      filter.pagination.page = (options[:page] || 1).to_i
    end
  end

  class TreemapDisplay < Display
    def key
      'treemap'
    end
  end

  DISPLAYS = [ListDisplay.new, TreemapDisplay.new]

  def self.register_display(display)
    DISPLAYS << display
  end

  # Simple hash {string key => fixnum or boolean or string}
  attr_accessor :criteria

  # Configuration available after call to execute()
  attr_reader :pagination, :security_exclusions, :columns, :display

  # Results : sorted array of Data
  attr_reader :data

  def initialize(criteria={})
    @criteria = criteria
    @pagination = Api::Pagination.new
  end

  def sort_key
    @criteria[:sort]
  end

  def sort_asc?
    @criteria[:asc]=='true'
  end

  # ==== Options
  # 'page' : page id starting with 1. Used in display 'list'.
  # 'user' : the authenticated user
  # 'period' : index of the period between 1 and 5
  #
  def execute(controller, options={})
    return reset_results if @criteria.empty?
    init_display
    init_filter(options)

    user = options[:user]
    rows=Api::Utils.java_facade.executeMeasureFilter2(@criteria, (user ? user.id : nil))
    snapshot_ids = filter_authorized_snapshot_ids(rows, controller)
    snapshot_ids = paginate_snapshot_ids(snapshot_ids)
    init_data(snapshot_ids)

    self
  end

  def set_criteria_value(key, value)
    if value
      @criteria[key.to_sym]=value.to_s
    else
      @criteria.delete(key.to_sym)
    end
  end

  def set_criteria_default_value(key, value)
    if !@criteria.has_key?(key.to_sym)
      set_criteria_value(key, value)
    end
  end

  private

  def init_display
    key = @criteria[:display]
    if key.present?
      @display = DISPLAYS.find { |display| display.key==key }
    end
    @display ||= DISPLAYS.first
  end

  def init_filter(options)
    @display.prepare_filter(self, options)
    @columns = @criteria[:columns].split(',').map { |col_key| Column.new(col_key) }
  end

  def reset_results
    @pagination = Api::Pagination.new
    @security_exclusions = nil
    @data = nil
    self
  end

  def filter_authorized_snapshot_ids(rows, controller)
    project_ids = rows.map { |row| row.getResourceRootId() }.compact.uniq
    authorized_project_ids = controller.select_authorized(:user, project_ids)
    snapshot_ids = rows.map { |row| row.getSnapshotId() if authorized_project_ids.include?(row.getResourceRootId()) }.compact
    @security_exclusions = (snapshot_ids.size<rows.size)
    snapshot_ids
  end

  def paginate_snapshot_ids(snapshot_ids)
    @pagination.count = snapshot_ids.size
    snapshot_ids[@pagination.offset .. (@pagination.offset+@pagination.limit)]
  end

  def init_data(snapshot_ids)
    @data = []
    if !snapshot_ids.empty?
      data_by_snapshot_id = {}
      snapshots = Snapshot.find(:all, :include => ['project'], :conditions => ['id in (?)', snapshot_ids])
      snapshots.each do |snapshot|
        data = Data.new(snapshot)
        data_by_snapshot_id[snapshot.id] = data
        @data << data
      end

      metric_ids = @columns.map { |column| column.metric }.compact.uniq.map { |metric| metric.id }
      unless metric_ids.empty?
        measures = ProjectMeasure.find(:all, :conditions =>
          ['rule_priority is null and rule_id is null and characteristic_id is null and person_id is null and snapshot_id in (?) and metric_id in (?)', snapshot_ids, metric_ids]
        )
        measures.each do |measure|
          data = data_by_snapshot_id[measure.snapshot_id]
          data.add_measure measure
        end
      end

      if @columns.index { |column| column.links? }
        project_ids = []
        data_by_project_id = {}
        snapshots.each do |snapshot|
          project_ids << snapshot.project_id
          data_by_project_id[snapshot.project_id] = data_by_snapshot_id[snapshot.id]
        end
        links = ProjectLink.find(:all, :conditions => {:project_id => project_ids}, :order => 'link_type')
        links.each do |link|
          data_by_project_id[link.project_id].add_link(link)
        end
      end
    end
  end

end
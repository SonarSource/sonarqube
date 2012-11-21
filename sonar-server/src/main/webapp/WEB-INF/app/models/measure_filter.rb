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
# License along with {library}; if not, write to the Free Software
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
    attr_reader :key, :metric

    def initialize(key)
      @key = key
      metric_key = @key.split(':')[1]
      @metric = Metric.by_key(metric_key) if metric_key
    end

    def name?
      @key == 'name'
    end

    def links?
      @key == 'links'
    end
  end

  class Display
    def init_filter(filter)

    end
  end

  class ListDisplay < Display
    def key
      'list'
    end

    def init_filter(filter)

    end
  end

  class TreemapDisplay < Display
    def key
      'treemap'
    end
  end

  DEFAULT_OPTIONS = {:page => 1, :page_size => 50}
  DEFAULT_COLUMNS = [Column.new('name'), Column.new('date'), Column.new('metric:ncloc'), Column.new('metric:violations')]
  DISPLAYS = [ListDisplay.new, TreemapDisplay.new]

  # Simple hash {string key => fixnum or boolean or string}
  attr_accessor :criteria

  # Configuration available after call to execute()
  attr_reader :pagination, :security_exclusions, :columns, :display

  # Results : sorted array of Data
  attr_reader :data

  def initialize(criteria={})
    @criteria = criteria
  end

  # ==== Options
  # 'page' : page id starting with 1. Used on table display.
  # 'page_size' : number of results per page.
  # 'user' : the authenticated user
  # 'period' : index of the period between 1 and 5
  #
  def execute(controller, options={})
    return reset_results if @criteria.empty?
    init_columns
    init_display(options)

    opts = DEFAULT_OPTIONS.merge(options)
    user = opts[:user]

    rows=Api::Utils.java_facade.executeMeasureFilter2(@criteria, (user ? user.id : nil))
    snapshot_ids = filter_authorized_snapshot_ids(rows, controller)
    snapshot_ids = paginate_snapshot_ids(snapshot_ids, opts)
    init_data(snapshot_ids)

    self
  end

  private

  def init_columns
    fields = @criteria['columns']
    if fields.present?
      @columns = fields.split(',').map { |field| Column.new(field) }
    else
      @columns = DEFAULT_COLUMNS.clone
    end
  end

  def init_display(options)
    key = @criteria['display']
    if key.present?
      @display = DISPLAYS.find { |display| display.key==key }
    end
    @display ||= DISPLAYS.first
  end

  def reset_results
    @pagination = nil
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

  def paginate_snapshot_ids(snapshot_ids, options)
    @pagination = Api::Pagination.new({
                                        :per_page => options[:page_size],
                                        :page => options[:page],
                                        :count => snapshot_ids.size})
    from = (@pagination.page - 1) * @pagination.per_page
    to = (@pagination.page * @pagination.per_page) - 1
    to = snapshot_ids.size - 1 if to >= snapshot_ids.size
    snapshot_ids[from..to]
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
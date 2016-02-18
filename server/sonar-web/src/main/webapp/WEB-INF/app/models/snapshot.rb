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
class Snapshot < ActiveRecord::Base
  include Resourceable
  acts_as_tree :foreign_key => 'parent_snapshot_id'

  belongs_to :project
  belongs_to :root_project, :class_name => 'Project', :foreign_key => 'root_project_id'
  belongs_to :parent_snapshot, :class_name => 'Snapshot', :foreign_key => 'parent_snapshot_id'
  belongs_to :root_snapshot, :class_name => 'Snapshot', :foreign_key => 'root_snapshot_id'

  has_many :measures, :class_name => 'ProjectMeasure', :conditions => 'rule_id IS NULL AND person_id IS NULL'
  has_many :rulemeasures, :class_name => 'ProjectMeasure', :conditions => 'rule_id IS NOT NULL AND person_id IS NULL', :include => 'rule'
  has_many :person_measures, :class_name => 'ProjectMeasure', :conditions => 'rule_id IS NULL AND person_id IS NOT NULL'

  has_many :events, :dependent => :destroy, :order => 'event_date DESC'

  STATUS_UNPROCESSED = 'U'
  STATUS_PROCESSED = 'P'
  
  def created_at
    long_to_date(:created_at)
  end
  
  def build_date
    long_to_date(:build_date)
  end
  
  def period1_date
    long_to_date(:period1_date)
  end
  
  def period2_date
    long_to_date(:period2_date)
  end
  
  def period3_date
    long_to_date(:period3_date)
  end
  
  def period4_date
    long_to_date(:period4_date)
  end
  
  def period5_date
    long_to_date(:period5_date)
  end
  
  def long_to_date(attribute)
    date_in_long = read_attribute(attribute)
    Time.at(date_in_long/1000) if date_in_long
  end

  def self.for_timemachine_matrix(resource)
    # http://jira.sonarsource.com/browse/SONAR-1850
    # Conditions on scope and qualifier are required to exclude library snapshots.
    # Use-case :
    #   1. project A 2.0 is analyzed -> new snapshot A with qualifier TRK
    #   2. project B, which depends on A 1.0, is analyzed -> new snapshot A 1.0 with qualifier LIB.
    #   3. project A has 2 snapshots : the first one with qualifier=TRK has measures, the second one with qualifier LIB has no measures. Its version must not be used in time machine
    # That's why the 2 following SQL requests check the qualifiers (and optionally scopes, just to be sure)
    snapshots=Snapshot.find(:all, :conditions => ["snapshots.project_id=? AND events.snapshot_id=snapshots.id AND snapshots.status=? AND snapshots.scope=? AND snapshots.qualifier=?", resource.id, STATUS_PROCESSED, resource.scope, resource.qualifier],
                            :include => 'events',
                            :order => 'snapshots.created_at ASC')

    snapshots<<resource.last_snapshot if snapshots.empty?

    snapshots=snapshots[-5, 5] if snapshots.size>=5

    snapshots.insert(0, Snapshot.find(:first,
                                      :conditions => ["project_id=? AND status=? AND scope=? AND qualifier=?", resource.id, STATUS_PROCESSED, resource.scope, resource.qualifier],
                                      :include => 'project', :order => 'snapshots.created_at ASC', :limit => 1))

    snapshots.compact.uniq
  end

  def self.for_timemachine_widget(resource, number_of_columns, options={})
    if number_of_columns == 1
      # Display only the latest snapshot
      return [resource.last_snapshot]
    end

    # Get 1rst & latests snapshots of the period
    snapshot_conditions = ["snapshots.project_id=? AND snapshots.status=? AND snapshots.scope=? AND snapshots.qualifier=?", resource.id, STATUS_PROCESSED, resource.scope, resource.qualifier]
    if options[:from]
      snapshot_conditions[0] += " AND snapshots.created_at>=?"
      snapshot_conditions << options[:from].to_i * 1000
    end
    first_snapshot=Snapshot.find(:first, :conditions => snapshot_conditions, :order => 'snapshots.created_at ASC')
    last_snapshot=resource.last_snapshot

    if first_snapshot==last_snapshot
      return [last_snapshot]
    end

    # Look for the number_of_columns-2 last snapshots to display  (they must have 'Version' events)
    version_snapshots = []
    if number_of_columns > 2
      snapshot_conditions[0] += " AND events.snapshot_id=snapshots.id AND events.category='Version' AND snapshots.id NOT IN (?)"
      snapshot_conditions << [first_snapshot.id, last_snapshot.id]
      version_snapshots=Snapshot.find(:all, :conditions => snapshot_conditions, :include => 'events', :order => 'snapshots.created_at ASC').last(number_of_columns-2)
    end

    return [first_snapshot] + version_snapshots + [last_snapshot]
  end

  def last?
    islast
  end

  def root_snapshot
    @root_snapshot ||=
      (root_snapshot_id ? Snapshot.find(root_snapshot_id) : self)
  end

  def project_snapshot
    @project_snapshot ||=
      begin
        if scope==Project::SCOPE_SET
          self
        elsif parent_snapshot_id
          parent_snapshot.project_snapshot
        else
          nil
        end
      end
  end

  def root?
    parent_snapshot_id.nil?
  end

  def descendants
    children.map(&:descendants).flatten + children
  end

  def user_events
    categories=EventCategory.categories(true)
    category_names=categories.map { |cat| cat.name }
    Event.find(:all, :conditions => ["snapshot_id=? AND category IS NOT NULL", id], :order => 'event_date desc').select do |event|
      category_names.include?(event.category)
    end
  end

  def event(category)
    result=events.select { |e| e.category==category }
    if result.empty?
      nil
    else
      result.first
    end
  end

  def measure(metric)
    unless metric.is_a? Metric
      metric=Metric.by_key(metric)
    end
    metric ? measures_hash[metric.id] : nil
  end

  def person_measure(metric, person_id)
    person_measures.each do |m|
      return m if m.metric_id==metric.id && m.person_id==person_id
    end
    nil
  end

  def f_measure(metric)
    m=measure(metric)
    m && m.formatted_value
  end

  def rule_measures(metrics=nil, rule=nil)
    if metrics
      metric_ids=[metrics].flatten.map { |metric| metric.id }
    end
    if metrics || rule
      rulemeasures.select do |m|
        (metric_ids.nil? || metric_ids.include?(m.metric_id)) && (rule.nil? || m.rule_id==rule.id)
      end
    else
      rulemeasures
    end
  end

  def rule_measure(metric, rule)
    rulemeasures.each do |m|
      return m if m.metric_id==metric.id && m.rule_id==rule.id
    end
    nil
  end

  def self.snapshot_by_date(resource_id, date)
    if resource_id && date
      Snapshot.find(:first, :conditions => ['created_at>=? and created_at<? and project_id=?', date.beginning_of_day.to_i*1000, date.end_of_day.to_i*1000, resource_id], :order => 'created_at desc')
    else
      nil
    end
  end

  def resource
    project
  end

  def resource_id
    project_id
  end

  def periods?
    (period1_mode || period2_mode || period3_mode || period4_mode || period5_mode) != nil
  end

  def resource_id_for_authorization
    root_project_id || project_id
  end

  def path_name
    result=''
    if root_snapshot_id && root_snapshot_id!=id
      result += root_snapshot.project.long_name
      result += ' &raquo; '
      if root_snapshot.depth<self.depth-2
        result += ' ... &raquo; '
      end
      if parent_snapshot_id && root_snapshot_id!=parent_snapshot_id
        result += "#{parent_snapshot.project.long_name} &raquo; "
      end
    end
    result += project.long_name
    result
  end

  def period_mode(period_index)
    project_snapshot.send "period#{period_index}_mode"
  end

  def period_param(period_index)
    project_snapshot.send "period#{period_index}_param"
  end

  def period_datetime(period_index)
    project_snapshot.send "period#{period_index}_date"
  end

  def metrics
    @metrics ||=
      begin
        measures_hash.keys.map { |metric_id| Metric::by_id(metric_id) }.uniq.compact
      end
  end

  # metrics of all the available measures
  def metric_keys
    @metric_keys ||=
      begin
        metrics.map { |m| m.name }
      end
  end

  def has_source
    SnapshotSource.count('id', :conditions => "snapshot_id = #{id}") > 0
  end

  private

  def measures_hash
    @measures_hash ||=
      begin
        hash = {}
        measures.each do |measure|
          hash[measure.metric_id]=measure
        end
        hash
      end
  end

end

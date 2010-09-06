#
# Sonar, entreprise quality control tool.
# Copyright (C) 2009 SonarSource SA
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
class Snapshot < ActiveRecord::Base
  include Resourceable
  acts_as_tree :foreign_key => 'parent_snapshot_id'

  belongs_to :project
  belongs_to :root_project, :class_name => 'Project', :foreign_key => 'root_project_id'
  belongs_to :parent_snapshot, :class_name => 'Snapshot', :foreign_key => 'parent_snapshot_id'
  belongs_to :root_snapshot, :class_name => 'Snapshot', :foreign_key => 'root_snapshot_id'
  belongs_to :characteristic

  has_many :measures, :class_name => 'ProjectMeasure', :conditions => 'rule_id IS NULL AND rules_category_id IS NULL AND rule_priority IS NULL AND characteristic_id IS NULL'
  has_many :rulemeasures, :class_name => 'ProjectMeasure', :conditions => '(rule_id IS NOT NULL OR rules_category_id IS NOT NULL OR rule_priority IS NOT NULL) AND characteristic_id IS NULL'
  has_many :characteristic_measures, :class_name => 'ProjectMeasure', :conditions => 'rule_id IS NULL AND rules_category_id IS NULL AND rule_priority IS NULL AND characteristic_id IS NOT NULL'
  
  has_many :events, :dependent => :destroy, :order => 'event_date DESC'
  has_one :source, :class_name => 'SnapshotSource', :dependent => :destroy

  has_many :async_measure_snapshots
  has_many :async_measures, :through => :async_measure_snapshots


  STATUS_UNPROCESSED = 'U'
  STATUS_PROCESSED = 'P'

  def self.last_enabled_projects
    Snapshot.find(:all, 
      :include => 'project',
      :conditions => ['snapshots.islast=? and projects.scope=? and projects.qualifier=? and snapshots.scope=? and snapshots.qualifier=?',
        true, Project::SCOPE_SET, Project::QUALIFIER_PROJECT, Project::SCOPE_SET, Project::QUALIFIER_PROJECT])
  end
  
  def self.for_timemachine_matrix(resource_id)
    snapshots=Snapshot.find(:all, :conditions => ["snapshots.project_id=? AND events.snapshot_id=snapshots.id AND snapshots.status=?", resource_id, STATUS_PROCESSED],
       :include => 'events',
       :order => 'snapshots.created_at ASC')

    snapshots=snapshots[-5,5] if snapshots.size>=5

    snapshots.insert(0, Snapshot.find(:first,
         :conditions => ["project_id = :project_id AND status IN (:status)", {:project_id => resource_id, :status => STATUS_PROCESSED}],
         :include => 'project', :order => 'snapshots.created_at ASC', :limit => 1))
    snapshots.compact.uniq
  end

  def last?
    islast
  end

  def root_snapshot
    @root_snapshot ||=
      (root_snapshot_id ? Snapshot.find(root_snapshot_id) : self)
  end
  
  def project_snapshot
    if scope==Project::SCOPE_SET
      self
    elsif parent_snapshot_id
      parent_snapshot.project
    else
      nil
    end
  end

  def root?
    parent_snapshot_id.nil?
  end

  def all_measures
    measures + async_measures
  end
  
  def descendants
    children.map(&:descendants).flatten + children
  end

  def user_events
    categories=EventCategory.categories(true)
    category_names=categories.map{|cat| cat.name}
    Event.find(:all, :conditions => ["snapshot_id=? AND category IS NOT NULL", id], :order => 'event_date desc').select do |event|
      category_names.include?(event.category)
    end
  end

  def event(category)
    result=events.select{|e| e.category==category}
    if result.empty?
      nil
    else
      result.first
    end
  end

  def metrics
    if @metrics.nil?
      @metrics = []
      measures_hash.each_key do |metric_id|
        @metrics << Metric::by_id(metric_id)
      end
      @metrics.uniq!
    end
    @metrics
  end

  def measure(metric)
    unless metric.is_a? Metric
      metric=Metric.by_key(metric)
    end
    metric ? measures_hash[metric.id] : nil
  end

  def characteristic_measure(metric, characteristic)
    characteristic_measures.each do |m|
      return m if m.metric_id==metric.id && m.characteristic==characteristic
    end
    nil
  end

  def f_measure(metric)
    m=measure(metric)
    m ? m.formatted_value : nil
  end

  def rule_measures(metric=nil, rule_categ_id=nil, rule_priority=nil)
    rulemeasures.select do |m|
      m.rule_id && (metric ? m.metric_id==metric.id : true) && (rule_priority ? m.rule_priority==rule_priority : true) && (rule_categ_id ? m.rules_category_id==rule_categ_id : true)
    end
  end
  
  def rule_category_measures(metric_key)
    rulemeasures.select do |measure|
      measure.rule_id.nil? && measure.rule_priority.nil? && measure.rules_category_id && measure.metric && measure.metric.key==metric_key
    end
  end
  
  def rule_priority_measures(metric_key)
    rulemeasures.select do |measure|
      measure.rule_id.nil? && measure.rule_priority && measure.rules_category_id.nil? && measure.metric && measure.metric.key==metric_key
    end
  end

  def self.snapshot_by_date(resource_id, date)
    if resource_id and date
      Snapshot.find(:first, :conditions => ['created_at>=? and created_at<? and project_id=?', date.beginning_of_day, date.end_of_day, resource_id], :order => 'created_at desc')
    else
      nil
    end
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

  private

  def measures_hash
    @measures_hash ||=
      begin
        hash = {}
        all_measures.each do |measure|
          hash[measure.metric_id]=measure
        end
        hash
      end
  end

end

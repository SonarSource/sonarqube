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
class Sonar::TreemapBuilder 
  DEFAULT_WIDTH = 280
  DEFAULT_HEIGHT = 280

  CONFIGURATION_DEFAULT_COLOR_METRIC = 'sonar.core.treemap.colormetric'
  CONFIGURATION_DEFAULT_SIZE_METRIC = 'sonar.core.treemap.sizemetric'
  
  def self.size_metrics(options={})
    exclude_user_managed=options[:exclude_user_managed]||false
    Metric.all.select{ |metric| 
      metric.enabled && !metric.hidden && metric.numeric? && !metric.domain.blank? && (!exclude_user_managed || !metric.user_managed?)
    }.sort
  end

  def self.color_metrics
    Metric.all.select{ |metric|
      metric.enabled && !metric.hidden && ((metric.numeric? && metric.worst_value && metric.best_value) || metric.val_type==Metric::VALUE_TYPE_LEVEL)
    }.sort
  end
  
  def self.build(snapshots, width=DEFAULT_WIDTH, height=DEFAULT_HEIGHT, size_metric_key=nil, color_metric_key=nil)
    size_metric = Metric.by_key(size_metric_key) || default_size_metric
    color_metric = Metric.by_key(color_metric_key) || default_color_metric

    if snapshots.empty?
      measures = []
    else
      # temporary fix for SONAR-1098
      snapshots=snapshots[0...999]
      measures = ProjectMeasure.find(:all,
        :conditions => ['rules_category_id IS NULL and rule_id IS NULL and rule_priority IS NULL and metric_id IN (?) and snapshot_id IN (?)',
          [size_metric.id, color_metric.id], snapshots.map{|s| s.id}])
    end
    Sonar::Treemap.new(measures_hash_by_snapshot(snapshots, measures), width, height, size_metric, color_metric)
  end

  private
  
  def self.default_color_metric
    metric=Metric.by_key(Property.value(CONFIGURATION_DEFAULT_COLOR_METRIC))
    if metric.nil?
      metric = Metric.by_key(Metric::VIOLATIONS_DENSITY)
    end 
    metric
  end
  
  def self.default_size_metric
    metric=Metric.by_key(Property.value(CONFIGURATION_DEFAULT_SIZE_METRIC))
    if metric.nil?
      metric = Metric.by_key(Metric::NCLOC)
    end
    metric
  end
  
  def self.measures_hash_by_snapshot(snapshots, measures)
    snapshot_by_id = {}
    snapshots.each {|s| snapshot_by_id[s.id]=s}
    hash={}
    measures.each do |m|
      hash[snapshot_by_id[m.snapshot_id]] ||= {}
      hash[snapshot_by_id[m.snapshot_id]][m.metric]=m
    end
    hash
  end

  def self.measures_by_snapshot(snapshots, measures)
    snapshot_by_id = {}
    snapshots.each {|s| snapshot_by_id[s.id]=s}
    hash={}
    measures.each do |m|
      hash[snapshot_by_id[m.snapshot_id]] ||= []
      hash[snapshot_by_id[m.snapshot_id]] << m
    end
    hash
  end
end
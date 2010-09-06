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
class ProjectMeasure < ActiveRecord::Base
  include ActionView::Helpers::NumberHelper

  SEC = 1000 * 1
  MIN = 1000 * 60
  HOUR = 1000 * 60 * 60
  DAY = 1000 * 60 * 60 * 24

  belongs_to :snapshot
  belongs_to :rule
  belongs_to :rules_category
  belongs_to :project
  belongs_to :characteristic
  has_one :measure_data, :class_name => 'MeasureData', :foreign_key => 'measure_id'

  has_many :async_measure_snapshots
  has_many :snapshots, :through => :async_measure_snapshots

  validates_numericality_of :value, :if => :numerical_metric?
  validate :validate_date, :validate_value

  def metric
    @metric ||=
      begin
        Metric.by_id(metric_id)
      end
  end
  
  def metric=(m)
    @metric = m
    write_attribute(:metric_id, m.id) if m.id
  end
  
  def category
    @category ||=
      begin
        RulesCategory.by_id(rules_category_id)
      end
  end
  
  def category=(c)
    @category = RulesCategory.by_name(c.name)
    write_attribute(:rules_category_id, @category.id) if @category.id
  end
  
  def rule_measure?
    rule_id || rules_category_id || rule_priority
  end

  def data
    if metric.data?
      text_value || (measure_data ? measure_data.data : nil)
    else
      text_value
    end
  end

  def formatted_value
    if metric.nil?
      return value.to_s
    end

    case metric().val_type
    when Metric::VALUE_TYPE_INT
      number_with_precision(value(), :precision => 0)
    when Metric::VALUE_TYPE_FLOAT
      number_with_precision(value(), :precision => 1)
    when Metric::VALUE_TYPE_PERCENT
      number_to_percentage(value(), {:precision => 1})
    when Metric::VALUE_TYPE_MILLISEC
      millisecs_formatted_value( value() )
    when Metric::VALUE_TYPE_BOOLEAN
      value() == 1 ? 'Yes' : 'No'
    when Metric::VALUE_TYPE_LEVEL
      text_value
    when Metric::VALUE_TYPE_STRING
      text_value
    else
      value().to_s
    end
  end

  def millisecs_formatted_value( value )
    # bugfix with jruby 1.0 release does not support % for BigDecimal
    value = value.to_i if value.kind_of? BigDecimal
    if value >= HOUR
      hours = ( value / HOUR ).to_i
      mins = ( value % HOUR / MIN ).to_i
      secs = ( value % MIN / SEC ).to_i
      return hours.to_s + ":" + leading_zero( mins ) + ":" + leading_zero( secs ) + " h"
    elsif value >= MIN
      mins = ( value / MIN ).to_i
      secs = ( value % MIN / SEC ).to_i
      millisecs = ( value % MIN % SEC ).to_i
      return mins.to_s + ":" + leading_zero( secs ) + " min"
    elsif value >= SEC
      secs = ( value / SEC ).to_i
      millisecs = ( value % SEC ).to_i
      return secs.to_s + "." + millisecs.to_s[0, 1] + " sec" if millisecs != 0
      return secs.to_s+ " sec" if millisecs == 0
    else
      ( "%d" % value ) + " ms"
    end
  end

  def leading_zero( value )
    if ( value < 10 )
      return "0" + value.to_s
    end
    return value.to_s
  end

  def self.sort_by_metric_direction(measures, metric)
    if measures
      measures.sort! do |x, y|
        x.value <=> y.value
      end
      measures.reverse! if (metric && metric.direction<0)
    end
    measures
  end

  # return reviews from the snapshot and also reviews created after the snapshot
  def self.find_reviews_for_last_snapshot(snapshot)
    ProjectMeasure.find(:all, :include => [:async_measure_snapshots, :measure_data],
      :conditions => ['async_measure_snapshots.project_id=project_measures.project_id AND ' +
        '((async_measure_snapshots.snapshot_id IS NULL AND project_measures.measure_date>?) ' +
        'OR async_measure_snapshots.snapshot_id=?) ', snapshot.created_at, snapshot.id])
  end

  def self.find_reviews_for_last_snapshot_with_opts(snapshot, options)
    metrics = options[:metrics].nil? ? [] : Metric.ids_from_keys(options[:metrics].split(','))
    include = (options[:includeparams] == "true") ? [:async_measure_snapshots, :measure_data] : :async_measure_snapshots

    metrics_conditions = (metrics.empty?) ? "" : "AND project_measures.metric_id IN (?)"
    conditions = 'async_measure_snapshots.project_id=project_measures.project_id AND ' +
      '((async_measure_snapshots.snapshot_id IS NULL AND project_measures.measure_date>?) ' +
      'OR async_measure_snapshots.snapshot_id=?) ' + metrics_conditions
    if metrics.empty?
      ProjectMeasure.find(:all, :include => include, :conditions => [conditions, snapshot.created_at, snapshot.id])
    else
      ProjectMeasure.find(:all, :include => include, :conditions => [conditions, snapshot.created_at, snapshot.id, metrics])
    end
  end

  def self.find_reviews(snapshot)
    conditions = 'async_measure_snapshots.snapshot_id=? ' + metrics_conditions
    ProjectMeasure.find(:all, :include => [:async_measure_snapshots, :measure_data],
      :conditions => ['async_measure_snapshots.snapshot_id=? ', snapshot.id])
  end


  def self.find_reviews_with_opts(snapshot, options={})
    metrics = options[:metrics].nil? ? [] : Metric.ids_from_keys(options[:metrics].split(','))
    include = (options[:includeparams] == "true") ? [:async_measure_snapshots, :measure_data] : :async_measure_snapshots

    metrics_conditions = (metrics.empty?) ? "" : "AND project_measures.metric_id IN (?)"
    conditions = 'async_measure_snapshots.snapshot_id=? ' + metrics_conditions
    if metrics.empty?
      ProjectMeasure.find(:all, :include => include, :conditions => [conditions, snapshot.id])
    else
      ProjectMeasure.find(:all, :include => include, :conditions => [conditions, snapshot.id, metrics])
    end
  end

  def tip
    if rule_id
      rule.description
    else
      snapshot.project.tip
    end
  end

  def self.find_by_metrics_and_snapshots(metric_ids, snapshot_ids)
    ProjectMeasure.find(:all, :conditions => {:snapshot_id => snapshot_ids, :metric_id => metric_ids})
  end

  def self.find_by_metric_and_snapshot_ids(metric, snapshot_ids, rules_category_id=nil, rule_id=nil)
    parameters = {:snapshot_id => snapshot_ids, :metric_id => metric.id}
    parameters[:rule_id] = rule_id
    if (rule_id.nil?)
      parameters[:rules_category_id] = rules_category_id
    end

    (metric.direction<0) ? order_direction = 'desc' : order_direction = 'asc'
    ProjectMeasure.find(:all, :conditions => parameters, :order => "value #{order_direction}")
  end

  def review?
    measure_date != nil
  end

  def short_name
    metric.short_name
  end

  def rule_priority_text
    rule_priority ? Sonar::RulePriority.to_s(rule_priority) : nil
  end

  def key
    if rule_measure?
      if rule_id
        "#{metric_key}_rule_#{rule_id}"
      elsif rules_category_id
        "#{metric_key}_#{rules_category_id}"
      else
        "#{metric_key}_#{rule_priority_text}"
      end
    else
      metric_key
    end
  end
  
  def metric_key
    metric ? metric.name : nil
  end

  def tendency_qualitative
    if !metric.qualitative? || tendency.nil? || tendency==0 || metric.direction==0
      0
    elsif tendency>0
      metric.direction>0 ? 1 : -1
    else
      metric.direction<0 ? 1 : -1
    end
  end

  

  def <=>(other)
    return value<=>other.value
  end

  private

  def numerical_metric?
    [Metric::VALUE_TYPE_INT, Metric::VALUE_TYPE_FLOAT, Metric::VALUE_TYPE_PERCENT, Metric::VALUE_TYPE_MILLISEC].include?(metric.val_type)
  end

  def validate_date
    if not measure_date
      errors.add_to_base('A valid date must be provided')
    else
      last_snasphot_date = project.last_snapshot.created_at
      if project.last_snapshot.created_at < measure_date
        errors.add_to_base("The date should not be after #{last_snasphot_date.strftime('%Y-%m-%d')}")
      end
    end
  end

  def validate_value
    case metric.value_type
    when Metric::VALUE_TYPE_INT
      errors.add_to_base("A numerical value must be provided") if value.nil?
    when Metric::VALUE_TYPE_FLOAT
      errors.add_to_base("A numerical value must be provided") if value.nil?
    when Metric::VALUE_TYPE_PERCENT
      errors.add_to_base("A numerical value must be provided") if value.nil?
    when Metric::VALUE_TYPE_MILLISEC
      errors.add_to_base("Value must be greater than 0") if value < 0
    when Metric::VALUE_TYPE_BOOLEAN
      raw_value = send("value_before_type_cast")
      if raw_value.instance_of?(String)
        raw_value = raw_value.downcase
        errors.add_to_base("Value must be 'No' or 'Yes'") if raw_value != "yes" and raw_value != "no"
        write_attribute( "value", 1.0) if raw_value == "yes"
        write_attribute( "value", 0.0) if raw_value == "no"
      end
    when Metric::VALUE_TYPE_STRING
      errors.add_to_base("A text value must be provided") if text_value.blank?
    end
  end


end
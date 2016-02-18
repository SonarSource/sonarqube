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
class ProjectMeasure < ActiveRecord::Base
  include ActionView::Helpers::NumberHelper

  SEC = 1000 * 1
  MIN = 1000 * 60
  HOUR = 1000 * 60 * 60
  DAY = 1000 * 60 * 60 * 24

  belongs_to :snapshot
  belongs_to :rule
  belongs_to :project
  belongs_to :person, :class_name => 'Project', :foreign_key => 'person_id'

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

  def rule_measure?
    rule_id != nil
  end

  def data
    if metric.data?
      text_value || measure_data
    else
      text_value
    end
  end

  def data_as_line_distribution
    @line_distribution ||=
      begin
        hash={}
        if data
          parts=data.split(';')
          parts.each do |part|
            fields=part.split('=')
            hash[fields[0].to_i]=fields[1]
          end
        end
        hash
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
        number_with_precision(value(), :precision => (metric().decimal_scale||1))
      when Metric::VALUE_TYPE_PERCENT
        number_to_percentage(value(), {:precision => 1})
      when Metric::VALUE_TYPE_MILLISEC
        millisecs_formatted_value( value() )
      when Metric::VALUE_TYPE_WORK_DUR
        work_duration_formatted_value(value())
      when Metric::VALUE_TYPE_BOOLEAN
        value() == 1 ? 'Yes' : 'No'
      when Metric::VALUE_TYPE_LEVEL
        text_value
      when Metric::VALUE_TYPE_STRING
        text_value
      when Metric::VALUE_TYPE_RATING
        text_value || value.to_i.to_s
      else
        value().to_s
    end
  end

  def format_numeric_value(val, options={})
    if metric.nil?
      return val.to_s
    end

    case metric().val_type
      when Metric::VALUE_TYPE_INT
        number_with_precision(val, :precision => 0)
      when Metric::VALUE_TYPE_FLOAT
        number_with_precision(val, :precision => (metric().decimal_scale||1))
      when Metric::VALUE_TYPE_PERCENT
        if (options[:variation]==true)
          number_with_precision(val, :precision => (metric().decimal_scale||1))
        else
          number_to_percentage(val, {:precision => (metric().decimal_scale||1)})
        end
      when Metric::VALUE_TYPE_MILLISEC
        millisecs_formatted_value(val)
      when Metric::VALUE_TYPE_WORK_DUR
        work_duration_formatted_value(val)
      when Metric::VALUE_TYPE_RATING
        number_with_precision(val, :precision => 0)
      else
        val.to_s
    end
  end

  def variation(period_index)
    result = nil
    case period_index
    when 1
      result=variation_value_1
    when 2
      result=variation_value_2
    when 3
      result=variation_value_3
    when 4
      result=variation_value_4
    when 5
      result=variation_value_5
    end
    result
  end

  def millisecs_formatted_value( value )
    # bugfix with jruby 1.0 release does not support % for BigDecimal
    value = value.to_i
    if value.abs >= HOUR
      hours = ( value / HOUR ).to_i
      mins = ( value % HOUR / MIN ).to_i
      secs = ( value % MIN / SEC ).to_i
      return hours.to_s + ':' + leading_zero( mins ) + ':' + leading_zero( secs ) + ' h'
    elsif value.abs >= MIN
      mins = ( value / MIN ).to_i
      secs = ( value % MIN / SEC ).to_i
      return mins.to_s + ':' + leading_zero( secs ) + ' min'
    elsif value.abs >= SEC
      secs = ( value / SEC ).to_i
      ms = ( value % SEC ).to_i
      return secs.to_s + (ms < 100 ? '' : '.' + ms.to_s[-3,1]) + ' sec'
    else
      ( '%d' % value ) + ' ms'
    end
  end

  def work_duration_formatted_value(value)
    Internal.i18n.formatLongDuration(value.to_i, 'SHORT')
  end

  def color
    @color ||=
      begin
        MeasureColor.color(self)
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

  def typed_value
    case metric().val_type
    when Metric::VALUE_TYPE_INT
      (value && value.to_i)
    when Metric::VALUE_TYPE_FLOAT
      (value && value.to_f)
    when Metric::VALUE_TYPE_PERCENT
      (value && value.to_f)
    when Metric::VALUE_TYPE_MILLISEC
      (value && value.to_i)
    when Metric::VALUE_TYPE_BOOLEAN
      value
    when Metric::VALUE_TYPE_LEVEL
      text_value
    when Metric::VALUE_TYPE_STRING
      text_value
    when Metric::VALUE_TYPE_RATING
      text_value || (value && value.to_i)
    else
      text_value || (value && value.to_i)
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

  def short_name
    metric.short_name
  end

  # Deprecated in v.2.13. Replaced by severity()
  def rule_priority_text
    nil
  end

  # not used for a while. Deprecated in 5.2.
  def severity
    nil
  end

  def key
    if rule_measure?
      if rule_id
        "#{metric_key}_rule_#{rule_id}"
      end
    else
      metric_key
    end
  end

  def metric_key
    metric ? metric.name : nil
  end

  def tendency_qualitative
    # unsupported since version 5.2
    0
  end

  def <=>(other)
    value<=>other.value
  end

  def visible?(period)
    !(text_value.nil? && value.nil? && variation(period).nil?)
  end

end

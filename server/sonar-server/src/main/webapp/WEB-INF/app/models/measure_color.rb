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
class MeasureColor

  MIN_COLOR=Color::RGB.from_html("EE0000")   # red
  MEAN_COLOR=Color::RGB.from_html("FFEE00")   # orange
  MAX_COLOR=Color::RGB.from_html("00AA00")   # green
  NONE_COLOR=Color::RGB.from_html("DDDDDD")   # gray

  #
  # Options :
  #  * min : min value, else the metric worst_value
  #  * max : max value, else the metric best_value
  #  * period_index: integer between 1 and 5 if set, else nil
  #  * check_alert_status: true|false. Default is true.
  #
  def self.color(measure, options={})
    return NONE_COLOR if measure.nil?

    options = {:check_alert_status => true}.merge(options)
    max_value = options[:max] || measure.metric.best_value
    min_value = options[:min] || measure.metric.worst_value
    percent=-1.0

    if options[:period_index]
      if min_value && max_value
        value=measure.variation(options[:period_index])
        percent = value_to_percent(value, min_value, max_value)
      end
    else
      if options[:check_alert_status] && !measure.alert_status.blank?
        case(measure.alert_status)
          when Metric::TYPE_LEVEL_OK then percent=100.0
          when Metric::TYPE_LEVEL_ERROR then percent=0.0
          when Metric::TYPE_LEVEL_WARN then percent=50.0
        end
      elsif measure.metric.value_type==Metric::VALUE_TYPE_LEVEL
        case(measure.text_value)
          when Metric::TYPE_LEVEL_OK then percent=100.0
          when Metric::TYPE_LEVEL_WARN then percent=50.0
          when Metric::TYPE_LEVEL_ERROR then percent=0.0
        end
      elsif measure.value && max_value && min_value
        percent = value_to_percent(measure.value, min_value, max_value)
      end
    end

    max_color=options[:max_color]||MAX_COLOR
    min_color=options[:min_color]||MIN_COLOR
    mean_color=options[:mean_color]||MEAN_COLOR
    if percent<0.0
      NONE_COLOR
    elsif (percent > 50.0)
      max_color.mix_with(mean_color, (percent - 50.0) * 2.0)
    else
      min_color.mix_with(mean_color, (50.0 - percent) * 2.0)
    end
  end


  def self.value_to_percent(value, min, max)
    percent = 100.0 * (value.to_f - min.to_f) / (max.to_f - min.to_f)
    percent=100.0 if percent>100.0
    percent=0.0 if percent<0.0
    percent
  end
end

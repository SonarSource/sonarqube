#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2013 SonarSource
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
module AlertsHelper
  NUMERIC_THRESOLD_OPERATORS = ['<', '>', '=', '!=']
  BOOLEAN_THRESOLD_OPERATORS = ['=']
  STRING_THRESOLD_OPERATORS = ['=', '!=', '>', '<']
  LEVEL_THRESOLD_OPERATORS = ['=', '!=']

  def operators_for_alert
    NUMERIC_THRESOLD_OPERATORS
  end

  def operators_for_select(alert)
    if alert.metric.nil?
      {}
    elsif alert.metric.numeric?
      NUMERIC_THRESOLD_OPERATORS

    elsif alert.metric.val_type==Metric::VALUE_TYPE_BOOLEAN
      BOOLEAN_THRESOLD_OPERATORS

    elsif alert.metric.val_type==Metric::VALUE_TYPE_STRING
      STRING_THRESOLD_OPERATORS

    elsif alert.metric.val_type==Metric::VALUE_TYPE_LEVEL
      LEVEL_THRESOLD_OPERATORS
    else
      {}
    end
  end

  def default_operator(alert)
    if alert.metric.nil?
      nil
      
    elsif alert.metric.numeric?
      if alert.metric.qualitative?
        alert.metric.direction>0 ? '<' : '>'
      else
        '>'
      end

    elsif alert.metric.val_type==Metric::VALUE_TYPE_BOOLEAN
      '='

    elsif alert.metric.val_type==Metric::VALUE_TYPE_STRING
      '='

    elsif alert.metric.val_type==Metric::VALUE_TYPE_LEVEL
      '='

    else
      nil
    end
  end


  def value_field(alert, value, fieldname)
    if alert.metric.nil?
      text_field_tag fieldname, value, :size => 5
      
    elsif alert.metric.numeric?
      text_field_tag fieldname, value, :size => 5

    elsif alert.metric.val_type==Metric::VALUE_TYPE_BOOLEAN
      select_tag fieldname, options_for_select([['', ''], ['Yes', '1'], ['No', '0']], value)

    elsif alert.metric.val_type==Metric::VALUE_TYPE_STRING
      text_field_tag fieldname, value, :size => 5

    elsif alert.metric.val_type==Metric::VALUE_TYPE_LEVEL
      select_tag fieldname, options_for_select([['', ''], ['OK', Metric::TYPE_LEVEL_OK], ['Error', Metric::TYPE_LEVEL_ERROR], ['Warning', Metric::TYPE_LEVEL_WARN]], value)
    else
      hidden_field_tag fieldname, value
    end
  end

  def period_select_options(alert)
    if alert.metric
      select = ''
      select << period_select_option(alert, nil) if !alert.metric.name.start_with?("new_")
      for index in 1..3 do
        select << period_select_option(alert, index)
      end
      select
    end
  end

  def period_select_option(alert, index)
    if index
      selected = (alert.period == index ? 'selected' : '')
      "<option value='#{index}' #{selected}>#{period_label_index(index)}</option>"
    else
      selected = (alert.period.nil? || alert.period == 0 ? 'selected' : '')
      "<option value='0' #{selected}>#{message('value')}</option>"
    end
  end

  def period_label(alert)
    index = alert.period
    if index
      "#{period_label_index(index)}"
    else
      "#{message('value')}"
    end
  end

  def period_label_index(index)
    "&Delta; "+ Api::Utils.period_label(index)
  end


end
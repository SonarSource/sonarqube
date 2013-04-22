#
# Sonar, entreprise quality control tool.
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
class ManualMeasure < ActiveRecord::Base
  include ActionView::Helpers::NumberHelper
  
  belongs_to :resource, :class_name => 'Project'
  validates_uniqueness_of :metric_id, :scope => :resource_id
  validates_length_of :text_value, :maximum => 4000, :allow_nil => true, :allow_blank => true
  validates_length_of :description, :maximum => 4000, :allow_nil => true, :allow_blank => true
  validate :validate_metric, :validate_value

  def metric
    @metric ||=
        begin
          Metric.by_id(metric_id)
        end
  end

  def user
    @user ||=
        begin
          user_login ? User.find(:first, :conditions => ['login=?', user_login]) : nil
        end
  end

  def username
    user ? user.name : user_login
  end

  def metric=(m)
    @metric = m
    write_attribute(:metric_id, m.id) if m.id
  end

  def typed_value=(v)
    if metric && metric.numeric?
      self.value=v
    else
      self.text_value=v
    end
  end

  def pending?(snapshot=nil)
    if snapshot.nil?
      snapshot=resource.last_snapshot
    end
    snapshot && updated_at && snapshot.created_at<updated_at
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
        millisecs_formatted_value(value())
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

  def editable_value
    if metric.nil?
      return ''
    end

    if metric.value_type==Metric::VALUE_TYPE_INT
      value ? value.to_i.to_s : ''
    elsif metric.numeric?
      value ? value.to_s : ''
    elsif metric.value_type==Metric::VALUE_TYPE_BOOLEAN
      value ? (value==1 ? 'Yes' : 'No') : ''
    else
      text_value
    end
  end

  def validate_metric
    if metric.nil? || !metric.enabled?
      errors.add_to_base("Unknown metric")
    elsif !metric.user_managed?
      errors.add_to_base("Not a manual metric")
    end
  end

  def validate_value
    if metric
      case metric.value_type
        when Metric::VALUE_TYPE_INT
          errors.add('value', "An integer value must be provided") if value_before_type_cast.nil? || !Api::Utils.is_integer?(value_before_type_cast)
        when Metric::VALUE_TYPE_FLOAT
          errors.add('value', "A numerical value must be provided") if value_before_type_cast.nil? || !Api::Utils.is_number?(value_before_type_cast)
        when Metric::VALUE_TYPE_PERCENT
          errors.add('value', "A numerical value must be provided") if value_before_type_cast.nil? || !Api::Utils.is_number?(value_before_type_cast)
        when Metric::VALUE_TYPE_MILLISEC
          errors.add('value', "Value must equal or be greater than 0") if value_before_type_cast.nil? || !Api::Utils.is_number?(value_before_type_cast) || value<0
        when Metric::VALUE_TYPE_BOOLEAN
          raw_value = text_value.downcase
          errors.add('value', "Value must be 'No' or 'Yes'") if raw_value != "yes" && raw_value != "no"
          write_attribute("value", 1.0) if raw_value == "yes"
          write_attribute("value", 0.0) if raw_value == "no"
        when Metric::VALUE_TYPE_LEVEL
          raw_value = text_value.upcase
          errors.add('value', "Value must be OK, WARN or ERROR") if !['OK', 'WARN', 'ERROR'].include?(raw_value)
          write_attribute("value", Sonar::RulePriority.id(raw_value))
          write_attribute("text_value", raw_value)
      end
    end
  end

end
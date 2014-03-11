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
class ActiveRule < ActiveRecord::Base
  belongs_to :rules_profile, :class_name => 'Profile', :foreign_key => 'profile_id'
  belongs_to :rule
  has_many :active_rule_parameters, :dependent => :destroy

  def level
    failure_level
  end

  def priority
    failure_level
  end

  def priority_text
    Sonar::RulePriority.to_s failure_level
  end

  def error?
    Sonar::RulePriority::major?(failure_level)
  end

  def warning?
    Sonar::RulePriority::minor?(failure_level)
  end

  def info?
    Sonar::RulePriority::info?(failure_level)
  end

  def minor?
    Sonar::RulePriority::minor?(failure_level)
  end

  def major?
    Sonar::RulePriority::major?(failure_level)
  end

  def critical?
    Sonar::RulePriority::critical?(failure_level)
  end

  def blocker?
    Sonar::RulePriority::blocker?(failure_level)
  end

  def activated?
    error? || warning?
  end

  def value(param_id)
    active_rule_parameters.each do |param|
      return param.value if param.rules_parameter_id == param_id
    end
    nil
  end

  def parameters
    active_rule_parameters
  end

  def parameter(name)
    result=nil
    parameters.each do |param|
      result=param if (param.name==name)
    end
    result
  end

  def active_param_by_param_id(param_id)
    parameters.each do |param|
      return param if param.rules_parameter_id==param_id
    end
    nil
  end

  def inherited?
    inheritance=='INHERITED'
  end

  def overrides?
    inheritance=='OVERRIDES'
  end
end

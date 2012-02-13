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
class RulesParameter < ActiveRecord::Base

  validates_presence_of :name, :param_type

  PARAM_MAX_NUMBER = 4

  PARAM_TYPE_STRING = "s"
  PARAM_TYPE_STRING_LIST = "s{}"
  PARAM_TYPE_INTEGER = "i"
  PARAM_TYPE_INTEGER_LIST = "i{}"
  PARAM_TYPE_BOOLEAN = "b"
  PARAM_TYPE_REGEXP = "r"

  belongs_to :rule

  def is_set_type
    param_type.at(1) == "[" && param_type.ends_with?("]")
  end

  def get_allowed_tokens
    param_type[2, param_type.length-3].split(",")
  end

  def description
    @l10n_description ||=
        begin
          result = Java::OrgSonarServerUi::JRubyFacade.getInstance().getRuleParamDescription(I18n.locale, rule.repository_key, rule.plugin_rule_key, name())
          result = read_attribute(:description) unless result
          result
        end
  end

  def description=(value)
    write_attribute(:description, value)
  end

  def readable_param_type
    return "String" if param_type == PARAM_TYPE_STRING
    return "Set of string (, as delimiter)" if param_type == PARAM_TYPE_STRING_LIST
    return "Number" if param_type == PARAM_TYPE_INTEGER
    return "Set of number (, as delimiter)" if param_type == PARAM_TYPE_INTEGER_LIST
    return "Boolean" if param_type == PARAM_TYPE_BOOLEAN
    return "Regular expression" if param_type == PARAM_TYPE_REGEXP
    return "Set of values (, as delimiter)" if is_set_type
  end

  def input_box_size
    return 15 if param_type == PARAM_TYPE_STRING or param_type == PARAM_TYPE_STRING_LIST or param_type == PARAM_TYPE_REGEXP
    return 8 if param_type == PARAM_TYPE_INTEGER or param_type == PARAM_TYPE_INTEGER_LIST
    return 4 if param_type == PARAM_TYPE_BOOLEAN
    if is_set_type
      size = (param_type.length / 2).to_i
      size = 64 if size > 64
      return size
    end
  end

  def validate_value(attribute, errors, value)
    return if attribute.nil? or attribute.length == 0
    if is_set_type
      provided_tokens = attribute.split(",")
      allowed_tokens = get_allowed_tokens
      provided_tokens.each do |provided_token|
        if !allowed_tokens.include?(provided_token)
          errors.add("#{value}", "Invalid value '" + provided_token + "'. Must be one of : " + allowed_tokens.join(", "))
        end
      end
    elsif param_type == RulesParameter::PARAM_TYPE_INTEGER
      begin
        Kernel.Integer(attribute)
      rescue
        errors.add("#{value}", "Invalid value '" + attribute + "'. Must be an integer.")
      end
    elsif param_type == RulesParameter::PARAM_TYPE_INTEGER_LIST
      provided_numbers = attribute.split(",")
      provided_numbers.each do |provided_number|
        begin
          Kernel.Integer(provided_number)
        rescue
          errors.add("#{value}", "Invalid value '" + provided_number + "'. Must be an integer.")
          return
        end
      end
    elsif param_type == RulesParameter::PARAM_TYPE_BOOLEAN
      if attribute != "true" && attribute != "false"
        errors.add("#{value}", "Invalid value '" + attribute + "'. Must be one of : true,false")
      end
    elsif param_type == RulesParameter::PARAM_TYPE_REGEXP
      begin
        Regexp.new(attribute)
      rescue
        errors.add("#{value}", "Invalid regular expression '" + attribute + "'.")
      end
    end
  end

  def to_hash_json(active_rule)
    json = {'name' => name}
    json['description']=description if description
    if active_rule
      active_parameter = active_rule.active_param_by_param_id(id)
      json['value'] = active_parameter.value if active_parameter
    end
    json
  end

  def to_xml(active_rule, xml)
    xml.param do
      xml.name(name)
      xml.description { xml.cdata!(description) } if description
      if active_rule
        active_parameter = active_rule.active_param_by_param_id(id)
        xml.value(active_parameter.value) if active_parameter
      end
    end
  end

  def <=>(other)
    name <=> other.name
  end
end

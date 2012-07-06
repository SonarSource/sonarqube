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
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
module RulesConfigurationHelper
  include PropertiesHelper

  PARAM_TYPE_STRING = "s"
  PARAM_TYPE_STRING_LIST = "s{}"
  PARAM_TYPE_INTEGER = "i"
  PARAM_TYPE_INTEGER_LIST = "i{}"
  PARAM_TYPE_BOOLEAN = "b"
  PARAM_TYPE_REGEXP = "r"

  # Kept for compatibility with old rule param type
  def type_with_compatibility(type)
    return PropertyType::TYPE_STRING if type == PARAM_TYPE_STRING
    return PropertyType::TYPE_STRING if type == PARAM_TYPE_STRING_LIST
    return PropertyType::TYPE_INTEGER if type == PARAM_TYPE_INTEGER
    return PropertyType::TYPE_INTEGER if type == PARAM_TYPE_INTEGER_LIST
    return PropertyType::TYPE_BOOLEAN if type == PARAM_TYPE_BOOLEAN
    return PropertyType::TYPE_STRING if type == PARAM_TYPE_REGEXP
    return PropertyType::TYPE_STRING if is_set(type)

    type
  end

  def readable_type(type)
    return "Set of string (, as delimiter)" if type == PARAM_TYPE_STRING_LIST
    return "Number" if type_with_compatibility(type) == PropertyType::TYPE_INTEGER
    return "Set of number (, as delimiter)" if type == PARAM_TYPE_INTEGER_LIST
    return "Regular expression" if type == PARAM_TYPE_REGEXP
    return "Set of values (, as delimiter)" if is_set(type)
    ""
  end

  def param_value_input(parameter, value, options = {})
    property_value 'value', type_with_compatibility(parameter.param_type), value, {:id => parameter.id}.update(options)
  end

  def is_set(type)
    type.at(1) == "[" && type.ends_with?("]")
  end

  def validate_rule_param(attribute, param_type, errors, value)
    return if attribute.nil? or attribute.length == 0

    type=type_with_compatibility(param_type)

    if is_set_type
      allowed_tokens = get_allowed_tokens
      attribute.split(',').each do |provided_token|
        if !allowed_tokens.include?(provided_token)
          errors.add("#{value}", "'#{provided_token}' kust be one of : " + allowed_tokens.join(', '))
        end
      end
    elsif param_type == RulesConfigurationHelper::PARAM_TYPE_INTEGER_LIST
      attribute.split(',').each do |n|
        if !Api::Utils.is_integer?(n)
          errors.add("#{value}", "'#{n}' must be an integer.")
          return
        end
      end
    elsif param_type == RulesConfigurationHelper::PARAM_TYPE_REGEXP
      if !Api::Utils.is_regexp?(attribute)
        errors.add("#{value}", "'#{attribute}' must be a regular expression")
      end
    elsif type == PropertyType::TYPE_INTEGER
      if !Api::Utils.is_integer?(attribute)
        errors.add("#{value}", "'#{attribute}' must be an integer.")
      end
    elsif type == PropertyType::TYPE_BOOLEAN
      if !Api::Utils.is_boolean?(attribute)
        errors.add("#{value}", "'#{attribute}' must be one of : true,false")
      end
    end
  end
end


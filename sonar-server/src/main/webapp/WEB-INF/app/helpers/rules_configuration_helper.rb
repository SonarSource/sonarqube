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
module RulesConfigurationHelper
  include PropertiesHelper

  PARAM_TYPE_STRING_LIST = "s{}"
  PARAM_TYPE_INTEGER_LIST = "i{}"

  # Kept for compatibility with old rule param type
  def type_with_compatibility(type)
    return PropertyType::TYPE_STRING if type == 's'
    return PropertyType::TYPE_STRING if type == PARAM_TYPE_STRING_LIST
    return PropertyType::TYPE_INTEGER if type == 'i'
    return PropertyType::TYPE_INTEGER if type == PARAM_TYPE_INTEGER_LIST
    return PropertyType::TYPE_BOOLEAN if type == 'b'
    return PropertyType::TYPE_REGULAR_EXPRESSION if type == 'r'
    return PropertyType::TYPE_STRING if is_set(type)

    type
  end

  def readable_type(param_type)
    type=type_with_compatibility(param_type)

    return "Set of comma delimited strings" if param_type == PARAM_TYPE_STRING_LIST
    return "Number" if type == PropertyType::TYPE_INTEGER
    return "Set of comma delimited numbers" if param_type == PARAM_TYPE_INTEGER_LIST
    return "Regular expression" if type == PropertyType::TYPE_REGULAR_EXPRESSION
    return "Set of comma delimited values" if is_set(param_type)
    ""
  end

  def param_value_input(parameter, value, options = {})
    type=type_with_compatibility(parameter.param_type)
    name = options[:name] || 'value'
    property_input_field name, type, value, 'WIDGET', {:id => parameter.id, :size => options[:size] }.update(options)
  end

  def is_set(type)
    type.at(1) == "[" && type.ends_with?("]")
  end

  def validate_rule_param(attribute, param_type, errors, value)
    return if attribute.nil? or attribute.length == 0

    type=type_with_compatibility(param_type)

    if is_set_type
      attribute.split(',').each do |v|
        if !get_allowed_tokens.include?(v)
          errors.add("#{value}", "'#{v}' must be one of : " + get_allowed_tokens.join(', '))
        end
      end
    elsif param_type == RulesConfigurationHelper::PARAM_TYPE_INTEGER_LIST
      attribute.split(',').each do |n|
        if !Api::Utils.is_integer?(n)
          errors.add("#{value}", "'#{n}' must be an integer.")
        end
      end
    elsif type == PropertyType::TYPE_REGULAR_EXPRESSION
      errors.add("#{value}", "'#{attribute}' must be a regular expression") unless Api::Utils.is_regexp?(attribute)
    elsif type == PropertyType::TYPE_INTEGER
      errors.add("#{value}", "'#{attribute}' must be an integer.") unless Api::Utils.is_integer?(attribute)
    elsif type == PropertyType::TYPE_BOOLEAN
      errors.add("#{value}", "'#{attribute}' must be one of : true,false") unless Api::Utils.is_boolean?(attribute)
    end
  end
end


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
module RulesConfigurationHelper
  include PropertiesHelper

  def property_type(type)
    value_type = type.type
    return PropertyType::TYPE_STRING if value_type == 'STRING'
    return PropertyType::TYPE_INTEGER if value_type == 'INTEGER'
    return PropertyType::TYPE_BOOLEAN if value_type == 'BOOLEAN'
    return PropertyType::TYPE_STRING if value_type == 'SINGLE_SELECT_LIST'
    value_type
  end

  def readable_type(type)
    property_type = property_type(type)

    return "Number" if property_type == PropertyType::TYPE_INTEGER
    return "Set of comma delimited values" if type.type() == PropertyType::TYPE_SINGLE_SELECT_LIST
    ""
  end

  def param_value_input(rule, parameter, value, options = {})
    property_type = property_type(parameter.type())
    name = options[:name] || 'value'
    property_input_field name, property_type, value, 'WIDGET', {:id => "#{rule.id().to_s}#{parameter.key().to_s}", :size => options[:size]}.update(options)
  end

  def rule_key(qProfileRule)
    "#{qProfileRule.repositoryKey().to_s}:#{qProfileRule.key().to_s}"
  end

  def html_text(text)
    Api::Utils.markdown_to_html(text)
  end

  def plain_text(text)
    Api::Utils.convert_string_to_unix_newlines(text)
  end

end


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
class PropertyType
  TYPE_STRING = 'STRING'
  TYPE_TEXT = 'TEXT'
  TYPE_PASSWORD = 'PASSWORD'
  TYPE_BOOLEAN = 'BOOLEAN'
  TYPE_INTEGER = 'INTEGER'
  TYPE_FLOAT = 'FLOAT'
  TYPE_SINGLE_SELECT_LIST = 'SINGLE_SELECT_LIST'
  TYPE_METRIC = 'METRIC'
  TYPE_LICENSE = 'LICENSE'
  TYPE_REGULAR_EXPRESSION = 'REGULAR_EXPRESSION'

  TYPE_FILTER = 'FILTER'
  # Since 3.7
  TYPE_ISSUE_FILTER = 'ISSUE_FILTER'
  TYPE_USER_LOGIN = 'USER_LOGIN'

  def self.text_to_value(text, type)
    case type
      when TYPE_INTEGER
        text.to_i
      when TYPE_FLOAT
        Float(text)
      when TYPE_BOOLEAN
        text=='true'
      when TYPE_METRIC
        Metric.by_key(text)
      else
        text
    end
  end

  def self.validate(key, type, optional, text_value, errors)
    errors.add_to_base("Unknown type for property #{key}") unless type
    if text_value.empty?
      errors.add_to_base("#{key} is empty") unless optional
      return
    end

    errors.add_to_base("#{key} is not an integer") if type==TYPE_INTEGER && !Api::Utils.is_integer?(text_value)
    errors.add_to_base("#{key} is not a decimal number") if type==TYPE_FLOAT && !Api::Utils.is_number?(text_value)
    errors.add_to_base("#{key} is not a boolean") if type==TYPE_BOOLEAN && !Api::Utils.is_boolean?(text_value)
    errors.add_to_base("#{key} is not a regular expression") if type==TYPE_REGULAR_EXPRESSION && !Api::Utils.is_regexp?(text_value)
  end
end

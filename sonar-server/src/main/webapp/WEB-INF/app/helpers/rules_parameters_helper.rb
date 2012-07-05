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
module RulesParametersHelper
  include PropertiesHelper

  PARAM_TYPE_STRING = "s"
  PARAM_TYPE_STRING_LIST = "s{}"
  PARAM_TYPE_INTEGER = "i"
  PARAM_TYPE_INTEGER_LIST = "i{}"
  PARAM_TYPE_BOOLEAN = "b"
  PARAM_TYPE_REGEXP = "r"

  def readable_type(type)
    return "String" if type == PARAM_TYPE_STRING
    return "Set of string (, as delimiter)" if type == PARAM_TYPE_STRING_LIST
    return "Number" if type == PARAM_TYPE_INTEGER
    return "Set of number (, as delimiter)" if type == PARAM_TYPE_INTEGER_LIST
    return "Boolean" if type == PARAM_TYPE_BOOLEAN
    return "Regular expression" if type == PARAM_TYPE_REGEXP
    return "Set of values (, as delimiter)" if is_set(type)
  end

  def input_size(type)
    return 15 if type == PARAM_TYPE_STRING
    return 15 if type == PARAM_TYPE_STRING_LIST
    return  8 if type == PARAM_TYPE_INTEGER
    return  8 if type == PARAM_TYPE_INTEGER_LIST
    return  4 if type == PARAM_TYPE_BOOLEAN
    return 15 if type == PARAM_TYPE_REGEXP
    if is_set(type)
      size = (type.length / 2).to_i
      size = 64 if size > 64
      size
    end
  end

  def is_set(type)
    type.at(1) == "[" && type.ends_with?("]")
  end
end


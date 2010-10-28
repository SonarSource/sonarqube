#
# Sonar, entreprise quality control tool.
# Copyright (C) 2009 SonarSource SA
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
module WidgetPropertiesHelper
  VALUE_TYPE_INT = 'INT'
  VALUE_TYPE_BOOLEAN = 'BOOL'
  VALUE_TYPE_FLOAT = 'FLOAT'
  VALUE_TYPE_STRING = 'STRING'
  VALUE_TYPE_REGEXP = 'REGEXP'
   
  def valid_property_value?(type, value, parameter="")
    if type==VALUE_TYPE_INT
      value.to_i.to_s == value

    elsif type==VALUE_TYPE_FLOAT
      true if Float(value) rescue false

    elsif type==VALUE_TYPE_BOOLEAN
      value=="1" || value=="0"

    elsif type==VALUE_TYPE_STRING
      true

    elsif type==VALUE_TYPE_REGEXP
      value.to_s.match(parameter) == nil ? false : true

    else
      false
    end
  end
   
  def property_value_field(type, fieldname, value, param_value="")
    val= param_value ? param_value : value
    
    if type==VALUE_TYPE_INT
      text_field_tag fieldname, val , :size => 10
      
    elsif type==VALUE_TYPE_FLOAT
      text_field_tag fieldname, val, :size => 10

    elsif type==VALUE_TYPE_BOOLEAN
      opts="<option value=''>Select value</option>"
      opts+="<option value='1'"+(val=="1" ? " selected" : "" )+">Yes</option>"
      opts+="<option value='0'"+(val=="0" ? " selected" : "" )+">No</option>"
      select_tag fieldname, opts

    elsif type==VALUE_TYPE_STRING
      text_field_tag fieldname, val, :size => 10

    elsif type==VALUE_TYPE_REGEXP
      text_field_tag fieldname, val, :size => 10
    else
      hidden_field_tag fieldname
    end
  end
    
end 
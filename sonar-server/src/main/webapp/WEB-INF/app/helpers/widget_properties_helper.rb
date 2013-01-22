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
module WidgetPropertiesHelper
  include PropertiesHelper

  def property_value_field(definition, value, widget)
    id = definition.type.name != PropertyType::TYPE_METRIC ? definition.key : "prop-#{widget.id}-#{widget.key.parameterize}-#{definition.key.parameterize}"
    options = {:values => definition.options, :id => id, :default => definition.defaultValue}
    options[:extra_values] = {:key => widget.key} if definition.type.name == PropertyType::TYPE_SINGLE_SELECT_LIST
    property_input_field definition.key, definition.type.name, value, 'WIDGET', options
  end

end

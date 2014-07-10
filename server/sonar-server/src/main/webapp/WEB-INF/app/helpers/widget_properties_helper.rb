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
module WidgetPropertiesHelper
  include PropertiesHelper

  def property_value_field(definition, value, widget)
    id = definition.type.name != PropertyType::TYPE_METRIC ? definition.key : "prop-#{widget.id}-#{widget.key.parameterize}-#{definition.key.parameterize}"
    options = {:values => definition.options, :id => id, :default => definition.defaultValue}
    options[:extra_values] = {:widget_key => widget.key} if definition.type.name == PropertyType::TYPE_SINGLE_SELECT_LIST
    property_input_field definition.key, definition.type.name, value, 'WIDGET', options
  end

  def default_value(property_def)
    defaultValue = property_def.defaultValue
    # Boolean type should always have a default value, if no one is provided it's force to false
    defaultValue = property_def.type.name == PropertyType::TYPE_BOOLEAN ? 'false' : property_def.defaultValue if defaultValue.blank?
    defaultValue = '********' if property_def.type.name == PropertyType::TYPE_PASSWORD
    defaultValue = Metric::name_for(defaultValue) if property_def.type.name == PropertyType::TYPE_METRIC
    defaultValue
  end

end

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
module SettingsHelper
  def category_name(category)
    message("property.category.#{category}", :default => category)
  end

  def property_name(property)
    message("property.#{property.key()}.name", :default => property.name())
  end

  def property_description(property)
    message("property.#{property.key()}.description", :default => property.description())
  end

  def property_help(property)
    message("property.#{property.key()}.help", :default => '')
  end

  def property_value(property)
    if property.multi_values
      Property.values(property.key, @resource ? @resource.id : nil)
    else
      Property.value(property.key, @resource ? @resource.id : nil, '')
    end
  end

  # for backward-compatibility with properties that do not define the type TEXT
  def property_type(property, value)
    unless property.fields.blank?
      return 'PROPERTY_SET_DEFINITION'
    end

    if property.getType().to_s=='STRING' && value && value.include?('\n')
      return 'TEXT'
    end

    property.getType()
  end

  def by_name(categories)
    Api::Utils.insensitive_sort(categories) { |category| category_name(category) }
  end

  def input_name(property)
    "settings[#{h property.key}]" + (property.multi_values ? '[]' : '')
  end
end

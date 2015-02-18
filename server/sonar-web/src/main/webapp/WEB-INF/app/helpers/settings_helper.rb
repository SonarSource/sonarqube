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
module SettingsHelper
  include PropertiesHelper

  def category_name(category)
    # Try with lowercase key then with original key for backward compatibility
    message("property.category.#{category.key}", :default => message("property.category.#{category.originalKey}", :default => category.originalKey))
  end

  def subcategory_name(category, subcategory)
    if (category.key == subcategory.key)
      # If subcategory == category then it is the default one
      category_name(category)
    else
      # Try with lowercase key then with original key for backward compatibility
      message("property.category.#{category.key}.#{subcategory.key}",
        :default => message("property.category.#{category.originalKey}.#{subcategory.originalKey}", :default => subcategory.originalKey))
    end
  end

  def property_name(property)
    message("property.#{property.key}.name", :default => property.name)
  end

  def property_description(property)
    message("property.#{property.key}.description", :default => property.description)
  end

  def field_name(property, field)
    message("field.#{property.key}.#{field.key}.name", :default => field.name)
  end

  def field_description(property, field)
    message("field.#{property.key}.#{field.key}.description", :default => field.description)
  end

  def key_field(property)
    property.fields.find { |f| f.key == 'key' }
  end

  def option_name(property, field, option)
    option_name_with_key(property.key, field && field.key, option, nil)
  end

  def category_help(category)
    message("category.#{category}.help", :default => '')
  end

  def category_desc(category)
    message("property.category.#{category.key}.description", :default => '')
  end

  def subcategory_desc(category, subcategory)
    message("property.category.#{category.key}.#{subcategory.key}.description", :default => '')
  end

  def property_value(property)
    if property.multi_values?
      Property.values(property.key, @resource && @resource.id)
    else
      Property.value(property.key, @resource && @resource.id, '')
    end
  end

  # for backward-compatibility with properties that do not define the type TEXT
  def property_type(property, value)
    unless property.fields.blank?
      return 'PROPERTY_SET_DEFINITION'
    end

    if property.type.to_s=='STRING' && value && value.include?('\n')
      return 'TEXT'
    end

    property.type
  end

  def by_property_index_or_name(properties)
    properties.sort_by { |p| [p.index, property_name(p).downcase] }
  end

  def input_name(property)
    name = "settings[#{h property.key}]"
    if property.multi_values?
      name += '[]'
    end
    name
  end
end

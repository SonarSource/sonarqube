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
class SettingsController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION

  SPECIAL_CATEGORIES=%w(email encryption server_id)

  verify :method => :post, :only => %w(update), :redirect_to => {:action => :index}
  before_filter :admin_required, :only => %w(index)

  def index
    load_properties()
  end

  def update
    resource_id = params[:resource_id]
    @resource = Project.by_key(resource_id) if resource_id

    access_denied if (@resource && !is_admin?(@resource))
    access_denied if (@resource.nil? && !is_admin?)

    load_properties()
    save_properties(resource_id)
    save_property_sets(resource_id)

    render :partial => 'settings/properties'
  end

  private

  # TODO: Validation
  def save_property_sets(resource_id)
    params[:property_sets].each do |key, value|
      value = drop_trailing_blank_values(value)

      # TODO: clear all
      Property.set(key, value.map { |v| v.gsub(',', '%2C') }.join(','), resource_id)

      fields = params[key]

      fields.each do |field_key, field_values|
        field_values.each_with_index do |field_value, index|
          set_key = value[index]
          if set_key
            Property.set(key + "." + set_key + "." + field_key, field_value, resource_id)
          end
        end
      end
    end
  end

  def save_properties(resource_id)
    @updated_properties = {}
    params[:settings].each do |key, value|
      if value.kind_of? Array
        value = drop_trailing_blank_values(value)
      end

      if value.blank?
        Property.clear(key, resource_id)
      else
        @updated_properties[key] = Property.set(key, value, resource_id)
      end
    end
  end

  def drop_trailing_blank_values(array)
    array.reverse.drop_while(&:blank?).reverse
  end

  def load_properties
    @category = params[:category] || 'general'

    if @resource.nil?
      definitions_per_category = java_facade.propertyDefinitions.globalPropertiesByCategory
    elsif @resource.project?
      definitions_per_category = java_facade.propertyDefinitions.projectPropertiesByCategory
    elsif @resource.module?
      definitions_per_category = java_facade.propertyDefinitions.modulePropertiesByCategory
    end

    @categories = definitions_per_category.keys + SPECIAL_CATEGORIES
    @definitions = definitions_per_category[@category] || []

    not_found('category') unless @categories.include? @category
  end

end

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
class SettingsController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION

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

    @updated_properties = {}
    update_properties(resource_id)
    update_property_sets(resource_id)

    render :partial => 'settings/properties'
  end

  private

  def update_properties(resource_id)
    (params[:settings] || []).each do |key, value|
      update_property(key, value, resource_id)
    end
  end

  def update_property_sets(resource_id)
    (params[:property_sets] || []).each do |key, set_keys|
      update_property_set(key, set_keys, params[key], resource_id, params[:auto_generate] && params[:auto_generate][key])
    end
  end

  def update_property_set(key, set_keys, fields_hash, resource_id, auto_generate)
    if auto_generate
      max = (Time.now.to_f * 100000).to_i
      set_keys.each_with_index do |v, index|
        if v.blank?
          max += 1;
          set_keys[index] = max.to_s
        end
      end
    end

    set_key_values = {}
    fields_hash.each do |field_key, field_values|
      field_values.zip(set_keys).each do |field_value, set_key|
        set_key_values[set_key] ||= {}
        set_key_values[set_key][field_key] = field_value
      end
    end

    set_keys.reject! { |set_key| set_key.blank? || (auto_generate && set_key_values[set_key].values.all?(&:blank?)) }

    Property.transaction do
      # Delete only property sets that are no more existing
      condition = "prop_key LIKE '" + key + ".%' AND "
      set_keys.each {|set_key| condition += "prop_key NOT LIKE ('#{key + '.' + set_key + '.%'}') AND "}
      if resource_id
        condition += 'resource_id=' + resource_id
      else
        condition += 'resource_id IS NULL'
      end
      Property.delete_all(condition)

      update_property(key, set_keys, resource_id)
      set_keys.each do |set_key|
        update_property("#{key}.#{set_key}.key", set_key, resource_id) unless auto_generate

        set_key_values[set_key].each do |field, value|
          update_property("#{key}.#{set_key}.#{field}", value, resource_id)
        end
      end
    end
  end

  def update_property(key, value, resource_id)
    @updated_properties[key] = Property.set(key, value, resource_id)
  end

  def load_properties
    definitions_per_category = java_facade.propertyDefinitions.propertiesByCategory(@resource.nil? ? nil : @resource.qualifier)
    processProperties(definitions_per_category)
  end

end

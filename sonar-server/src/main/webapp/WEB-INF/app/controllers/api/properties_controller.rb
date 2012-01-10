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

require "json"

class Api::PropertiesController < Api::RestController

  before_filter :admin_required, :only => [ :create, :update, :destroy ]

  def index
    properties = Property.find(:all, :conditions => ['resource_id is null and user_id is null']).select do |property|
      viewable?(property.key)
    end
    rest_render(properties)
  end

  def show
    key = params[:id]
    resource_id_or_key = params[:resource]
    if resource_id_or_key
      resource = Project.by_key(resource_id_or_key)
      if resource
        prop = Property.by_key(key, resource.id)
      else
        rest_status_ko('Resource [' + resource_id_or_key + '] does not exist', 404)
        return
      end
    else
      prop = Property.by_key(key)
    end
    if prop
      if viewable?(key)
        rest_render([prop])
      else
        rest_status_ko('You are not authorized to see this ressource', 401)
      end
    else
      rest_status_ko('Property [' + params[:id] + '] does not exist', 404)
    end
  end

  def create
    key = params[:id]
    value = params[:value] || request.raw_post
    resource_id_or_key = params[:resource]
    if resource_id_or_key
      resource = Project.by_key(resource_id_or_key)
      if resource
        resource_id_or_key = resource.id
      else
        rest_status_ko('Resource [' + resource_id_or_key + '] does not exist', 404)
        return
      end
    end
    if key
      begin
        Property.set(key, value, resource_id_or_key)
        rest_status_ok
      rescue Exception => ex
        rest_status_ko(ex.message, 400)
      end
    else
      rest_status_ko('Property key [' + params[:id] + '] is not valid', 400)
    end
  end

  def update
    key = params[:id]
    value = params[:value] || request.raw_post
    resource_id_or_key = params[:resource]
    if resource_id_or_key
      resource = Project.by_key(resource_id_or_key)
      if resource
        resource_id_or_key = resource.id
      else
        rest_status_ko('Resource [' + resource_id_or_key + '] does not exist', 404)
        return
      end
    end
    if key
      begin
        Property.set(key, value, resource_id_or_key)
        rest_status_ok
      rescue Exception => ex
        rest_status_ko(ex.message, 400)
      end
    else
      rest_status_ko('Property key [' + params[:id] + '] is not valid', 400)
    end
  end

  def destroy
    key = params[:id]
    resource_id_or_key = params[:resource]
    if resource_id_or_key
      resource = Project.by_key(resource_id_or_key)
      if resource
        resource_id_or_key = resource.id
      else
        rest_status_ko('Resource [' + resource_id_or_key + '] does not exist', 404)
        return
      end
    end
    if key
      begin
        Property.clear(key, resource_id_or_key)
        rest_status_ok
      rescue Exception => ex
        rest_status_ko(ex.message, 400)
      end
    else
      rest_status_ko('Property key [' + params[:id] + '] is not valid', 400)
    end
  end

  protected

  def rest_to_json(properties)
    JSON(properties.collect{|property| property.to_hash_json})
  end

  def rest_to_xml(properties)
    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.instruct!
    xml.properties do
      properties.each do |property|
        property.to_xml(xml)
      end
    end
  end

  private

  def viewable?(property_key)
    !property_key.to_s.index('.secured') || is_admin?
  end

end

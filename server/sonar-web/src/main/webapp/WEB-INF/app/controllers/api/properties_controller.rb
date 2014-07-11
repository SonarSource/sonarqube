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
class Api::PropertiesController < Api::ApiController

  before_filter :admin_required, :only => [:create, :update, :destroy]

  # GET /api/properties/index?[resource=<resource id or key>]
  # Does NOT manage default values.
  def index
    keys=Set.new
    properties=[]

    # project properties
    if params[:resource]
      resource=Project.by_key(params[:resource])
      if resource
        # bottom-up projects
        projects=[resource].concat(resource.ancestor_projects)
        projects.each do |project|
          Property.find(:all, :conditions => ['resource_id=? and user_id is null', project.id]).each do |prop|
            properties<<prop if keys.add? prop.key
          end
        end
      end
    end

    # global properties
    Property.find(:all, :conditions => 'resource_id is null and user_id is null').each do |prop|
      properties<<prop if keys.add? prop.key
    end

    # apply security
    properties = properties.select{|prop| allowed?(prop.key)}

    respond_to do |format|
      format.json { render :json => jsonp(to_json(properties)) }
      format.xml { render :xml => to_xml(properties) }
    end
  end

  # GET /api/properties/<key>[?resource=<resource>]
  def show
    key = params[:id]
    resource_id_or_key = params[:resource]
    if resource_id_or_key
      resource = Project.by_key(resource_id_or_key)
      not_found('resource not found') unless resource
      prop = Property.by_key(key, resource.id)
    else
      prop = Property.by_key(key)
    end
    unless prop
      # for backward-compatibility with versions <= 2.14 : keep status 200
      message = "Property not found: #{key}"
      return respond_to do |format|
        format.json { render :json => error_to_json(404, message), :status => 200 }
        format.xml { render :xml => error_to_xml(404, message), :status => 200 }
        format.text { render :text => message, :status => 200 }
      end
    end
    access_denied unless allowed?(key)
    respond_to do |format|
      format.json { render :json => jsonp(to_json([prop])) }
      format.xml { render :xml => to_xml([prop]) }
    end
  end

  # curl -u admin:admin -v -X POST http://localhost:9000/api/properties/foo?value=bar[&resource=<resource>]
  def create
    update
  end

  # curl -u admin:admin -v -X PUT http://localhost:9000/api/properties/foo?value=bar[&resource=<resource>]
  def update
    key = params[:id]
    bad_request('missing key') unless key.present?
    value = params[:value] || request.raw_post
    resource_id_or_key = params[:resource]
    if resource_id_or_key
      resource = Project.by_key(resource_id_or_key)
      not_found('resource not found') unless resource
      resource_id_or_key = resource.id
    end
    prop=Property.set(key, value, resource_id_or_key)
    if prop.nil?
      render_success('property created') # Cleared
    elsif prop.valid?
      render_success('property created')
    else
      render_bad_request(prop.validation_error_message)
    end
  end

  # curl -u admin:admin -v -X DELETE http://localhost:9000/api/properties/foo[?resource=<resource>]
  def destroy
    key = params[:id]
    bad_request('missing key') unless key.present?
    resource_id_or_key = params[:resource]
    if resource_id_or_key
      resource = Project.by_key(resource_id_or_key)
      if resource
        resource_id_or_key = resource.id
      else
        # TODO should we ignore this error ?
        not_found('resource not found')
      end
    end
    Property.clear(key, resource_id_or_key)
    render_success('property deleted')
  end

  private

  def to_json(properties)
    properties.collect { |property| property.to_hash_json }
  end

  def to_xml(properties)
    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.instruct!
    xml.properties do
      properties.each do |property|
        property.to_xml(xml)
      end
    end
  end

  def allowed?(property_key)
    !property_key.end_with?('.secured') || is_admin?
  end

end

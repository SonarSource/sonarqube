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

require 'json'

class Api::UserPropertiesController < Api::ApiController

  before_filter :login_required


  #
  # GET /api/user_properties
  # curl http://localhost:9000/api/user_properties -v -u admin:admin
  #
  def index
    properties = current_user.properties
    respond_to do |format|
      format.json { render :json => jsonp(properties_to_json(properties)) }
      format.xml  { render :xml => properties_to_xml(properties) }
      format.text { render :text => text_not_supported }
    end
  end

  #
  # GET /api/user_properties/<key>
  # curl http://localhost:9000/api/user_properties/<key> -v -u admin:admin
  #
  def show
    property = Property.by_key(params[:id], nil, current_user.id)
    if property
      respond_to do |format|
        format.json { render :json => jsonp(properties_to_json([property])) }
        format.xml  { render :xml => properties_to_xml([property]) }
        format.text { render :text => text_not_supported }
      end
    else
      render_error('Not found', 404)
    end
  end


  #
  # POST /api/user_properties?key=<key>&value=<value>
  # curl -d "key=foo&value=bar" http://localhost:9000/api/user_properties -v -u admin:admin
  #
  def create
    key = params[:key]
    value = params[:value]
    if key
      begin
        Property.clear(key, nil, current_user.id)
        property=Property.create(:prop_key => key, :text_value => value, :user_id => current_user.id)
        respond_to do |format|
          format.json { render :json => jsonp(properties_to_json([property])) }
          format.xml  { render :xml => properties_to_xml([property]) }
          format.text { render :text => text_not_supported }
        end

      rescue Exception => e
        render_error(e.message, 500)
      end
    else
      render_error('Bad request: missing key', 400)
    end
  end

  #
  # DELETE /api/user_properties/<key>
  # curl -X DELETE  http://localhost:9000/api/user_properties/<key> -v -u admin:admin
  #
  def destroy
    begin
      if params[:id]
        Property.clear(params[:id], nil, current_user.id)
      end
      render_success("Property deleted")
    rescue Exception => e
      logger.error("Fails to execute #{request.url} : #{e.message}")
      render_error(e.message)
    end
  end

  private

  def properties_to_json(properties=[])
    json=[]
    properties.each do |p|
      json<<p.to_hash_json
    end
    json
  end

  def properties_to_xml(properties, xml=Builder::XmlMarkup.new(:indent => 0))
    xml.properties do
      properties.each do |p|
        p.to_xml(xml)
      end
    end
  end

end

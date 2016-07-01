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

class Api::FavouritesController < Api::ApiController

  before_filter :login_required

  #
  # GET /api/favourites
  # curl http://localhost:9000/api/favourites -v -u admin:admin
  #
  def index
    respond_to do |format|
      format.json { render :json => jsonp(favourites_to_json(current_user.favourites)) }
      format.xml  { render :xml => favourites_to_xml(current_user.favourites) }
      format.text { render :text => text_not_supported }
    end
  end

  #
  # POST /api/favourites?key=<key>
  # curl -X POST http://localhost:9000/api/favourites?key=org.apache.struts:struts -v -u admin:admin
  #
  def create
    favourite=current_user.add_favourite(params[:key])
    if favourite
      respond_to do |format|
        format.json { render :json => jsonp(favourites_to_json([favourite])) }
        format.xml  { render :xml => favourites_to_xml([favourite]) }
        format.text { render :text => text_not_supported }
      end
    else
      render_error('Favourite not found', 404)
    end
  end

  #
  # DELETE /api/favourites/<key>
  # curl -X DELETE  http://localhost:9000/api/favourites/org.apache.struts:struts -v -u admin:admin
  #
  def destroy
    ok=current_user.delete_favourite(params[:id])
    render_success(ok ? "Favourite deleted" : "Favourite not found")
  end

  def favourites_to_json(favourites=[])
    json=[]
    favourites.each do |f|
      json<<favourite_to_json(f)
    end
    json
  end

  def favourite_to_json(favourite)
    hash={}
    hash['id']=favourite.id
    hash['key']=favourite.key
    hash['name']=favourite.name
    hash['scope']=favourite.scope
    hash['branch']=favourite.branch if favourite.branch
    hash['lname']=favourite.long_name if favourite.long_name
    hash['lang']=favourite.language if favourite.language
    hash['qualifier']=favourite.qualifier
    hash
  end

  def favourites_to_xml(favourites, xml=Builder::XmlMarkup.new(:indent => 0))
    xml.favourites do
      favourites.each do |f|
        xml.favourite do
          xml.id(f.id)
          xml.key(f.key)
          xml.name(f.name)
          xml.lname(f.long_name) if f.long_name
          xml.branch(f.branch) if f.branch
          xml.scope(f.scope)
          xml.qualifier(f.qualifier)
          xml.lang(f.language) if f.language
        end
      end
    end
  end

  def favourite_to_xml(favourite, xml=Builder::XmlMarkup.new(:indent => 0))
    xml.favourite do
      xml.id(f.id)
      xml.key(f.key)
      xml.name(f.name)
      xml.lname(f.long_name) if f.long_name
      xml.branch(f.branch) if f.branch
      xml.scope(f.scope)
      xml.qualifier(f.qualifier)
      xml.lang(f.language) if f.language
    end
  end
end

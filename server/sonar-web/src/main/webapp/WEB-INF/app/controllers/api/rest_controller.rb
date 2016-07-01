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

#
# DEPRECATED use Api::ApiController instead
#
class Api::RestController < ApplicationController
  
  def index
    safe_rest(:rest_call) 
  end
  
  def safe_rest(method_to_call)
    begin
      send("#{method_to_call.to_s}") 
    rescue Exception => error
      rest_status_ko(error.message, 500)
      logger.error(error)
    end
  end
  
  def rest_error(msg)
    rest_status_ko(msg, 400)
  end
  
  def rest_status_ok
    render :nothing => true, :status => 200
  end
   
  def rest_status_ko(msg, error_code)
    if params['format'] == 'json' && params['callback']
      json = {'err_code' => error_code,'err_msg' => msg}
      render :json => jsonp(JSON(json)), :status => 200
    else
      render :text => msg, :status => error_code
    end
  end

  def access_denied
    rest_status_ko('Unauthorized', 401)
  end

  def check_database_version
    if !(DatabaseVersion.uptodate?)
      rest_status_ko("Database version not up to date", 500)
    end
  end
  
  def rest_render(objects)
    respond_to do |format| 
      format.json{ render :json => rest_to_jsonp(objects) }
      format.xml { render :xml => rest_to_xml(objects) }
      format.text { render :text => rest_to_text(objects) }
    end
  end
  
  def rest_to_jsonp(objects)
    jsonp(rest_to_json(objects))
  end
  
  def jsonp(json_string)
    if params['callback']
       params['callback'] + '(' + json_string + ');'
    else
      json_string
    end
  end
  
  def rest_to_text(objects)
    "Not supported"
  end
  
  def rest_to_xml(objects)
    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.instruct!
    xml.not_supported
  end
  
  def rest_to_json(objects)
    JSON({:not_supported => 'true'})
  end
 
end

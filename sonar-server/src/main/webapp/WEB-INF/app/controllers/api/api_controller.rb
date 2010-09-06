#
# Sonar, entreprise quality control tool.
# Copyright (C) 2009 SonarSource SA
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
require 'json'
require 'time'
class Api::ApiController < ApplicationController
  
  protected 
  
  def text_not_supported
    "Not supported"
  end
  
  def xml_not_supported
    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.instruct!
    xml.not_supported
  end
  
  def json_not_supported
    JSON({:not_supported => true})
  end
  
  def jsonp(json)
    text=( (json.is_a? String) ? json : JSON(json))
      
    if params['callback']
       params['callback'] + '(' + text + ');'
    else
      text
    end
  end
  
  def access_denied
    render_error('Unauthorized', 401)
  end
  
  def render_error(msg, http_status=400)
    respond_to do |format| 
      format.json{ render :json => error_to_json(msg, http_status), :status => http_status }
      format.xml{ render :xml => error_to_xml(msg, http_status), :status => http_status}
      format.text{ render :text => msg, :status => http_status }
    end
  end

  def error_to_json(msg, error_code=nil)
    hash={}
    hash[:err_code]=error_code if error_code
    hash[:err_msg]=msg if msg
    jsonp(hash)
  end

  def error_to_xml(msg, error_code=nil)
    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.error do 
      xml.code(error_code) if error_code
      xml.msg(msg) if msg
    end
  end

  def render_success(msg)
    render_error(msg, 200)
  end
  
  def format_datetime(datetime)
    datetime.strftime("%Y-%m-%dT%H:%M:%S%z")
  end
  
  def parse_datetime(datetime_string, default_is_now=true)
    if datetime_string.blank?
      return (default_is_now ? Time.now : nil)
    end
    Time.parse(datetime_string)
  end

  class ApiException < Exception
    attr_reader :code, :msg
    def initialize(code, msg)
      @code = code
      @msg = msg
    end
  end
  
end


#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2011 SonarSource
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

  class ApiException < Exception
    attr_reader :code, :msg

    def initialize(code, msg)
      @code = code
      @msg = msg
    end
  end

  #
  # Override the error handling defined in parent ApplicationController
  #
  rescue_from Exception, :with => :render_error
  rescue_from ApiException, :with => :render_error
  rescue_from Errors::BadRequest, :with => :render_bad_request
  rescue_from ActionController::UnknownAction, :with => :render_not_found
  rescue_from ActionController::RoutingError, :with => :render_not_found
  rescue_from ActionController::UnknownController, :with => :render_not_found
  rescue_from ActiveRecord::RecordNotFound, :with => :render_not_found
  rescue_from Errors::NotFound, :with => :render_not_found
  rescue_from Errors::AccessDenied, :with => :render_access_denied

  
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
    text=((json.is_a? String) ? json : JSON(json))

    if params['callback']
      params['callback'] + '(' + text + ');'
    else
      text
    end
  end

  # deprecated. Use Api::Utils.format_datetime
  def format_datetime(datetime)
    Api::Utils.format_datetime(datetime)
  end

  # deprecated. Use Api::Utils.parse_datetime
  def parse_datetime(datetime_string, default_is_now=true)
    Api::Utils.parse_datetime(datetime_string, default_is_now)
  end

  def load_resource(resource_key, role=nil)
    resource=Project.by_key(resource_key)
    not_found("Resource not found: #{resource_key}") if resource.nil?
    access_denied if role && !has_role?(role, resource)
    resource
  end


  #
  #
  # Error handling is different than in ApplicationController
  #
  #

  def render_error(message, status)
    logger.error("Fail to render: #{request.url}", message) if status==500
    respond_to do |format|
      format.json { render :json => error_to_json(status, message), :status => status }
      format.xml { render :xml => error_to_xml(status, message), :status => status }
      format.text { render :text => message, :status => status }
    end
  end

  def render_not_found(error)
    render_error(error.message, 404)
  end

  def render_bad_request(error)
    render_error(error.message, 400)
  end

  def render_access_denied
    render_error('Unauthorized', 401)
  end

  def render_success(message=nil)
    render_error(message, 200)
  end

  def error_to_json(status, message=nil)
    hash={:err_code => status}
    hash[:err_msg]=message if message
    jsonp(hash)
  end

  def error_to_xml(status, message=nil)
    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.error do
      xml.code(status)
      xml.msg(message) if message
    end
  end


end


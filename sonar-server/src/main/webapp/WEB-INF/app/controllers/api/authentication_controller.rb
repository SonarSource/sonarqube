#
# Sonar, open source software quality management tool.
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
class Api::AuthenticationController < Api::ApiController
  skip_before_filter :check_authentication

  # prevent HTTP proxies from caching authentication status
  before_filter :set_cache_buster, :only => 'index'

  #
  # GET /api/authentication/index
  # curl http://localhost:9000/api/authentication/index -v -u admin:admin
  #
  def index
    hash={:valid => valid?}

    respond_to do |format|
      format.json { render :json => jsonp(hash) }
      format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'authentication') }
      format.text { render :text => text_not_supported }
    end
  end

  private

  def valid?
    logged_in? || (!force_authentication? && anonymous?)
  end

  def force_authentication?
    property = Property.find(:first, :conditions => {:prop_key => org.sonar.api.CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY, :resource_id => nil, :user_id => nil})
    property ? property.value == 'true' : false
  end

  def anonymous?
    !session.has_key?(:user_id)
  end

  def set_cache_buster
    response.headers["Cache-Control"] = "no-cache, no-store, max-age=0, must-revalidate"
    response.headers["Pragma"] = "no-cache"
    response.headers["Expires"] = "Fri, 01 Jan 1990 00:00:00 GMT"
  end

end

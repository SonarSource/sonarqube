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
class Api::AuthenticationController < Api::ApiController
  skip_before_filter :check_authentication

  # prevent HTTP proxies from caching authentication status
  before_filter :set_cache_buster

  #
  # GET /api/authentication/validate
  # curl http://localhost:9000/api/authentication/validate -v -u admin:admin
  #
  # Since v.3.3
  def validate
    hash={:valid => valid?}

    # make sure no authentication information is left by
    # this validation 
    reset_session
    cookies[:auth_token]

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
    property = Property.by_key(org.sonar.api.CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY)
    property ? property.value == 'true' : false
  end

  def anonymous?
    !session.has_key?('user_id')
  end

  def set_cache_buster
    response.headers["Cache-Control"] = "no-cache, no-store, max-age=0, must-revalidate"
    response.headers["Pragma"] = "no-cache"
    response.headers["Expires"] = "Fri, 01 Jan 1990 00:00:00 GMT"
  end

end

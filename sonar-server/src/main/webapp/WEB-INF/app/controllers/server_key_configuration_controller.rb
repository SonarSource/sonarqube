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
class ServerKeyConfigurationController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION
  PROPERTY_SERVER_KEY = 'sonar.server_key'
  PROPERTY_IP_ADDRESS = 'sonar.server_key.ip_address'
  PROPERTY_ORGANIZATION = 'sonar.organization'

  before_filter :admin_required
  verify :method => :post, :only => [:save], :redirect_to => {:action => :index}

  def index
    @server_key = Property.value(PROPERTY_SERVER_KEY)
    @organization = Property.value(PROPERTY_ORGANIZATION)
    @address = Property.value(PROPERTY_IP_ADDRESS)
    @valid_addresses = java_facade.getValidInetAddressesForServerKey()
    params[:layout]='false'
  end

  def save
    organization = params[:organization]
    Property.set(PROPERTY_ORGANIZATION, organization)

    ip_address=params[:address]
    Property.set(PROPERTY_IP_ADDRESS, ip_address)

    key = java_facade.generate_server_key(organization, ip_address)
    if key
      Property.set(PROPERTY_SERVER_KEY, key)
    else
      Property.clear(PROPERTY_SERVER_KEY)
      flash[:error] = 'Please set valid organization and IP address'
    end
  
    redirect_to :action => 'index'
  end
end

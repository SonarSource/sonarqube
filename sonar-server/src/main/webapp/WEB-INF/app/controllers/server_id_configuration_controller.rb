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
class ServerIdConfigurationController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION
  PROPERTY_SERVER_ID = 'sonar.server_id'
  PROPERTY_IP_ADDRESS = 'sonar.server_id.ip_address'
  PROPERTY_ORGANISATION = 'sonar.organisation'

  before_filter :admin_required
  verify :method => :post, :only => [:generate], :redirect_to => {:action => :index}

  def index
    @server_id = Property.value(PROPERTY_SERVER_ID)
    @organisation = Property.value(PROPERTY_ORGANISATION)
    @address = Property.value(PROPERTY_IP_ADDRESS)
    @valid_addresses = java_facade.getValidInetAddressesForServerId()
    @bad_id = false
    if @server_id.present?
      id = java_facade.generateServerId(@organisation, @address)
      @bad_id = (@server_id != id)
    end
    params[:layout]='false'
  end

  def generate
    organisation = params[:organisation]
    Property.set(PROPERTY_ORGANISATION, organisation)

    ip_address=params[:address]
    Property.set(PROPERTY_IP_ADDRESS, ip_address)

    id = java_facade.generateServerId(organisation, ip_address)
    if id
      Property.set(PROPERTY_SERVER_ID, id)
    else
      Property.clear(PROPERTY_SERVER_ID)
      flash[:error] = Api::Utils.message('server_id_configuration.generation_error')
    end
  
    redirect_to :action => 'index'
  end
end

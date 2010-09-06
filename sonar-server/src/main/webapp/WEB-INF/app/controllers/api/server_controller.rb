#
# Sonar, open source software quality management tool.
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
class Api::ServerController < Api::ApiController

  skip_before_filter :check_authentication, :except => 'system'
  before_filter :admin_required, :only => 'system'
  
  def key
    render :text => Java::OrgSonarServerPlatform::Platform.getServer().getId()
  end

  def version
    render :text => Java::OrgSonarServerPlatform::Platform.getServer().getVersion()
  end

  def index
    hash={:id => Java::OrgSonarServerPlatform::Platform.getServer().getId(), :version => Java::OrgSonarServerPlatform::Platform.getServer().getVersion()}
    respond_to do |format| 
      format.json{ render :json => jsonp(hash) }
      format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'server') }
      format.text { render :text => text_not_supported}
    end
  end
  
  def system
    @server=Server.new(java_facade)
    json=[
      {:system_info => server_properties_to_json(@server.system_info)},
      {:system_statistics => server_properties_to_json(@server.system_statistics)},
      {:database_statistics => server_properties_to_json(@server.database_statistics)},
      {:sonar_info => server_properties_to_json(@server.sonar_info)},
      {:sonar_plugins => server_properties_to_json(@server.sonar_plugins)},
      {:system_properties => server_properties_to_json(@server.system_properties)},
      ]
    
    respond_to do |format| 
      format.json{ render :json => jsonp(json) }
      format.xml { render :xml => xml_not_supported }
      format.text { render :text => text_not_supported}
    end
  end
  
  
  private
  
  def server_properties_to_json(properties)
    hash={}
    properties.each do |prop|
      hash[prop[0].to_s]=prop[1].to_s
    end
    hash
  end
end

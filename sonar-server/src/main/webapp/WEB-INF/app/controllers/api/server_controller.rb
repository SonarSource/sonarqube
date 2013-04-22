#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2013 SonarSource
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
class Api::ServerController < Api::ApiController

  skip_before_filter :check_authentication, :except => 'system'

  # prevent HTTP proxies from caching server status
  before_filter :set_cache_buster, :only => 'index'

  # execute database setup
  verify :method => :post, :only => [:setup, :index_projects]
  skip_before_filter :check_database_version, :setup

  def key
    render :text => Java::OrgSonarServerPlatform::Platform.getServer().getId()
  end

  def version
    render :text => Java::OrgSonarServerPlatform::Platform.getServer().getVersion()
  end

  def index
    hash={:id => Java::OrgSonarServerPlatform::Platform.getServer().getId(), :version => Java::OrgSonarServerPlatform::Platform.getServer().getVersion()}
    complete_with_status(hash)
    respond_to do |format|
      format.json{ render :json => jsonp(hash) }
      format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'server') }
      format.text { render :text => text_not_supported}
    end
  end

  def system
    access_denied unless has_role?(:admin)
    @server=Server.new
    json=[
      {:system_info => server_properties_to_json(@server.system_info)},
      {:system_statistics => server_properties_to_json(@server.system_statistics)},
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

  def setup
    begin
      # Ask the DB migration manager to start the migration
      # => No need to check for authorizations (actually everybody can run the upgrade)
      # nor concurrent calls (this is handled directly by DatabaseMigrationManager)  
      DatabaseMigrationManager.instance.start_migration
      
      current_status = DatabaseMigrationManager.instance.is_sonar_access_allowed? ? "ok" : "ko"
      
      hash={:status => current_status,
            :migration_status => DatabaseMigrationManager.instance.status,
            :message => DatabaseMigrationManager.instance.message}
      respond_to do |format|
        format.json{ render :json => jsonp(hash) }
        format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'setup') }
        format.text { render :text => hash[:status] }
      end
    rescue => e
      hash={:status => 'ko', :msg => e.message}
      respond_to do |format|
        format.json{ render :json => jsonp(hash) }
        format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'setup') }
        format.text { render :text => hash[:status] }
      end
    end
  end

  def index_projects
    access_denied unless has_role?(:admin)
    logger.info 'Indexing projects'
    Java::OrgSonarServerUi::JRubyFacade.getInstance().indexProjects()
    render_success('Projects indexed')
  end

  private

  def server_properties_to_json(properties)
    hash={}
    properties.each do |prop|
      hash[prop[0].to_s]=prop[1].to_s
    end
    hash
  end

  def complete_with_status(hash)
    if DatabaseMigrationManager.instance.is_sonar_access_allowed?
      hash[:status]='UP'
    elsif DatabaseMigrationManager.instance.migration_running?
      hash[:status]='MIGRATION_RUNNING'
    elsif DatabaseMigrationManager.instance.requires_migration?
      hash[:status]='SETUP'
    else
      # migration failed or not connected to the database 
      hash[:status]='DOWN'
      hash[:status_msg]=DatabaseMigrationManager.instance.message
    end
  end

  def set_cache_buster
    response.headers["Cache-Control"] = "no-cache, no-store, max-age=0, must-revalidate"
    response.headers["Pragma"] = "no-cache"
    response.headers["Expires"] = "Fri, 01 Jan 1990 00:00:00 GMT"
  end
end

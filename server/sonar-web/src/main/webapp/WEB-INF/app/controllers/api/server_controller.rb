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
class Api::ServerController < Api::ApiController

  skip_before_filter :check_authentication

  # prevent HTTP proxies from caching server status
  before_filter :set_cache_buster, :only => 'index'

  # execute database setup
  skip_before_filter :check_database_version, :setup

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

  def setup
    verify_post_request
    manager=DatabaseMigrationManager.instance
    begin
      # Ask the DB migration manager to start the migration
      # => No need to check for authorizations (actually everybody can run the upgrade)
      # nor concurrent calls (this is handled directly by DatabaseMigrationManager)
      manager.start_migration

      operational=manager.is_sonar_access_allowed?
      current_status = operational ? "ok" : "ko"
      hash={
        # deprecated fields
        :status => current_status,
        :migration_status => manager.status,

        # correct fields
        :operational => operational,
        :state => manager.status
      }
      hash[:message]=manager.message if manager.message
      hash[:startedAt]=manager.migration_start_time if manager.migration_start_time

      respond_to do |format|
        format.json{ render :json => jsonp(hash) }
        format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'setup') }
        format.text { render :text => hash[:status] }
      end
    rescue => e
      hash={
        # deprecated fields
        :status => 'ko',
        :msg => e.message,

        # correct fields
        :message => e.message,
        :state => manager.status
      }
      respond_to do |format|
        format.json{ render :json => jsonp(hash) }
        format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'setup') }
        format.text { render :text => hash[:status] }
      end
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

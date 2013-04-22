 #
 # Sonar, entreprise quality control tool.
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
class SetupController < ApplicationController
  skip_before_filter :check_database_version, :check_authentication

  SECTION=Navigation::SECTION_CONFIGURATION
  
  verify :method => :post, :only => [ :setup_database ], :redirect_to => { :action => :index }
    
  def index
    if !ActiveRecord::Base.connected?
      render :template => 'setup/dbdown', :layout => 'nonav'
    elsif DatabaseMigrationManager.instance.requires_migration?
      render :template => 'setup/form', :layout => 'nonav'
    elsif DatabaseMigrationManager.instance.migration_running?
      render :template => 'setup/migration_running', :layout => 'nonav'
    elsif DatabaseMigrationManager.instance.migration_failed?
      render :template => 'setup/failed', :layout => 'nonav'
    else
      # migration succeeded, or no need for migration
      render :template => 'setup/db_uptodate', :layout => 'nonav' 
    end
  end

  def setup_database
    # Ask the DB migration manager to start the migration
    # => No need to check for authorizations (actually everybody can run the upgrade)
    # nor concurrent calls (this is handled directly by DatabaseMigrationManager)  
    DatabaseMigrationManager.instance.start_migration
    # and return some text that will actually never be displayed
    render :text => DatabaseMigrationManager.instance.message
  end

  def maintenance
    render :template => 'setup/maintenance', :layout => 'nonav'
  end
  
end

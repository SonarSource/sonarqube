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

#
# Class that centralizes the management the DB migration
#

require 'singleton'
require 'thread'

class DatabaseMigrationManager
  
  # mixin the singleton module to ensure we have only one instance of the class
  # it will be accessible with "DatabaseMigrationManager.instance"
  include Singleton
  
  # the status of the migration
  @status
  MIGRATION_NEEDED = "MIGRATION_NEEDED"
  MIGRATION_RUNNING = "MIGRATION_RUNNING"
  MIGRATION_FAILED = "MIGRATION_FAILED"
  MIGRATION_SUCCEEDED = "MIGRATION_SUCCEEDED"
  NO_MIGRATION = "NO_MIGRATION"
  
  # the corresponding message that can be given to the user
  @message
  
  # the time when the migration was started
  @start_time
  
  def initialize
    if !ActiveRecord::Base.connected?
      @status = MIGRATION_FAILED
      @message = "Not connected to database."
    elsif DatabaseVersion.uptodate?
      @status = NO_MIGRATION
      @message = "Database is up-to-date, no migration needed."
    else
      if DatabaseVersion.production?
        @status = MIGRATION_NEEDED
        @message = "Migration required."
      else
        @status = MIGRATION_FAILED
        @message = "Upgrade is not supported. Please use a <a href=\"http://docs.codehaus.org/display/SONAR/Requirements\">production-ready database</a>."
      end
    end
  end
  
  def message
    @message
  end
  
  def status
    @status
  end
  
  def requires_migration?
    @status==MIGRATION_NEEDED
  end
  
  def migration_running?
    @status==MIGRATION_RUNNING
  end
  
  def migration_failed?
    @status==MIGRATION_FAILED
  end
  
  def is_sonar_access_allowed?
    @status==NO_MIGRATION || @status==MIGRATION_SUCCEEDED
  end
  
  def migration_start_time
    @start_time
  end
  
  def start_migration
    # Use an exclusive block of code to ensure that only 1 thread will be able to proceed with the migration
    can_start_migration = false
    Thread.exclusive do
      if requires_migration?
        @status = MIGRATION_RUNNING
        @message = "Database migration is currently running."
        can_start_migration = true
      end
    end
    
    if can_start_migration
      # launch the upgrade
      begin
        @start_time = Time.now
        DatabaseVersion.upgrade_and_start
        @status = MIGRATION_SUCCEEDED
        @message = "The migration succeeded."
      rescue Exception => e
        @status = MIGRATION_FAILED
        @message = "The migration failed: " + e.clean_message + ".<br/> Please check the logs."
        # rethrow the exception so that it is logged and so that the whole system knows that a problem occured
        raise
      end
    end    
  end
  
end

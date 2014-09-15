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

#
# Class that centralizes the management of resource deletions in Sonar Web UI
#

require 'singleton'
require 'thread'

class ResourceDeletionManager
  
  # mixin the singleton module to ensure we have only one instance of the class
  # it will be accessible with "ResourceDeletionManager.instance"
  include Singleton
  
  # the status of the migration
  @status
  AVAILABLE = "AVAILABLE"
  WORKING = "WORKING"
  
  # the corresponding message that can be given to the user
  @message
  
  # list of resources that could not be deleted because of a problem
  @failed_deletions
  
  # the time when the deletion was started
  @start_time
  
  def initialize
    reinit()
  end
  
  def reinit
    @message = nil
    @status = AVAILABLE
    @failed_deletions = []
  end
  
  def message
    @message
  end
  
  def currently_deleting_resources?
    @status==WORKING
  end
  
  def deletion_failures_occured?
    !failed_deletions.empty?
  end
  
  def failed_deletions
    @failed_deletions
  end
  
  def deletion_start_time
    @start_time
  end
  
  def delete_resources(resource_ids=[])
    # Use an exclusive block of code to ensure that only 1 thread will be able to proceed with the deletion
    can_start_deletion = false
    Thread.exclusive do
      unless currently_deleting_resources?
        reinit()
        @status = WORKING
        @message = Api::Utils.message('bulk_deletion.deletion_manager.deleting_resources')
        can_start_deletion = true
      end
    end
    
    if can_start_deletion
      if resource_ids.empty?
        @status = AVAILABLE
        @message = Api::Utils.message('bulk_deletion.deletion_manager.no_resource_to_delete')
      else
        Thread.new do
          Thread.current[:name] = "Bulk Deletion of Projects"
          @start_time = Time.now

          java_facade = Java::OrgSonarServerUi::JRubyFacade.getInstance()
          # launch the deletion
          resource_ids.each_with_index do |resource_id, index|
            resource = Project.first(:conditions => {:id => resource_id.to_i})
            @message = Api::Utils.message('bulk_deletion.deletion_manager.currently_deleting_x_out_of_x', :params => [(index+1).to_s, resource_ids.size.to_s])
            if resource && java_facade.getResourceTypeBooleanProperty(resource.qualifier, 'deletable')
              begin
                java_facade.deleteResourceTree(resource.key)
              rescue Exception => e
                @failed_deletions << resource.name
                # no need to rethrow the exception as it has been logged by the Java component
              end
            end
          end

          @status = AVAILABLE
          @message = Api::Utils.message('bulk_deletion.deletion_manager.deletion_completed')
          @message += ' ' + Api::Utils.message('bulk_deletion.deletion_manager.however_failures_occurred') unless @failed_deletions.empty?
        end
      end
    end    
  end
  
end

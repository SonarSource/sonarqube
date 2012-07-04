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
class BulkDeletionController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION

  before_filter :admin_required
  verify :method => :post, :only => [:delete_resources], :redirect_to => { :action => :index }

  def index
    deletion_manager = ResourceDeletionManager.instance
    
    if deletion_manager.currently_deleting_resources? || 
      (!deletion_manager.currently_deleting_resources? && deletion_manager.deletion_failures_occured?)
      # a mass deletion is happening or it has just finished with errors => display the message from the Resource Deletion Manager
      @deletion_manager = deletion_manager
      render :template => 'bulk_deletion/pending_deletions'
    else
      @selected_tab = params[:resource_type] || 'projects'
      
      # search if there are VIEWS or DEVS to know if we should display the tabs or not
      @should_display_views_tab = Project.count(:all, :conditions => {:qualifier => 'VW'}) > 0
      @should_display_devs_tab = Project.count(:all, :conditions => {:qualifier => 'DEV'}) > 0
      
      # Search for resources
      conditions = "scope=:scope AND qualifier=:qualifier"
      values = {:scope => 'PRJ'}
      qualifier = 'TRK'
      if @selected_tab == 'views'
        qualifier = 'VW'
      elsif @selected_tab == 'devs'
        qualifier = 'DEV'
      end
      values[:qualifier] = qualifier
      if params[:name_filter]
        conditions += " AND name LIKE :name"
        values[:name] = '%' + params[:name_filter].strip + '%'
      end
        
      @resources = Project.find(:all, :conditions => [conditions, values])
    end
  end
  
  def delete_resources
    resource_to_delete = params[:resources] || []
    resource_to_delete = params[:all_resources].split(',') if params[:all_resources] && !params[:all_resources].blank?
    
    # Ask the resource deletion manager to start the migration
    # => this is an asynchronous AJAX call
    ResourceDeletionManager.instance.delete_resources(resource_to_delete)
    
    # and return some text that will actually never be displayed
    render :text => ResourceDeletionManager.instance.message
  end

  def pending_deletions
    deletion_manager = ResourceDeletionManager.instance
    
    if deletion_manager.currently_deleting_resources? || 
      (!deletion_manager.currently_deleting_resources? && deletion_manager.deletion_failures_occured?)
      # display the same page again and again
      @deletion_manager = deletion_manager
    else
      redirect_to :action => 'index'
    end
  end
  
  def dismiss_message
    # It is important to reinit the ResourceDeletionManager so that the deletion screens can be available again
    ResourceDeletionManager.instance.reinit
    
    redirect_to :action => 'index'
  end

end

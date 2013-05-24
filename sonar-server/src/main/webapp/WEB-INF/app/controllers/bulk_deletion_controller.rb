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
class BulkDeletionController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION

  before_filter :admin_required

  def index
    if pending_mass_deletion?
      render :template => 'bulk_deletion/pending_deletions'
      return
    end
        
    @tabs = deletable_qualifiers
    
    @selected_tab = params[:resource_type]
    @selected_tab = 'TRK' unless @tabs.include?(@selected_tab)
    
    # Search for resources
    conditions = "resource_index.qualifier=:qualifier"
    values = {:qualifier => @selected_tab}
    if params[:name_filter] && !params[:name_filter].blank?
      conditions += " AND resource_index.kee LIKE :kee"
      values[:kee] = params[:name_filter].strip.downcase + '%'
    end

    conditions += " AND projects.enabled=:enabled"
    values[:enabled] = true
    @resources = Project.find(:all,
                              :select => 'distinct(resource_index.resource_id),projects.id,projects.name,projects.kee,projects.long_name',
                              :conditions => [conditions, values],
                              :joins => :resource_index)
    @resources = Api::Utils.insensitive_sort!(@resources){|r| r.name}
  end

  def ghosts
    if pending_mass_deletion?
      render :template => 'bulk_deletion/pending_deletions'
      return
    end
      
    @tabs = deletable_qualifiers
    
    conditions = "scope=:scope AND qualifier IN (:qualifiers) AND status=:status"
    values = {:scope => 'PRJ', :qualifiers => @tabs}
    unprocessed_project_ids = Snapshot.find(:all, :select => 'project_id', :conditions => [conditions, values.merge({:status => Snapshot::STATUS_UNPROCESSED})]).map(&:project_id).uniq
    already_processed_project_ids = Snapshot.find(:all, :select => 'project_id', :conditions => [conditions + " AND project_id IN (:pids)", values.merge({:status => Snapshot::STATUS_PROCESSED, :pids => unprocessed_project_ids})]).map(&:project_id).uniq
    
    @ghosts = Project.find(:all, :conditions => ["id IN (?)", unprocessed_project_ids - already_processed_project_ids])
    
    @ghosts_by_qualifier = {}
    @ghosts.each do |p|
      qualifier = p.qualifier
      if @ghosts_by_qualifier[qualifier]
        @ghosts_by_qualifier[qualifier] << p
      else
        @ghosts_by_qualifier[qualifier] = [p]
      end
    end
  end
  
  def delete_resources
    verify_post_request
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
      # => implicit render "pending_deletions.html.erb"
    else
      redirect_to :action => 'index'
    end
  end
  
  def dismiss_message
    # It is important to reinit the ResourceDeletionManager so that the deletion screens can be available again
    ResourceDeletionManager.instance.reinit
    
    redirect_to :action => 'index'
  end
  
  private
  
  # Tells if a mass deletion is happening or if it has finished with errors
  def pending_mass_deletion?
    deletion_manager = ResourceDeletionManager.instance
    deletion_manager.currently_deleting_resources? || (!deletion_manager.currently_deleting_resources? && deletion_manager.deletion_failures_occured?)
  end
  
  def deletable_qualifiers
    Java::OrgSonarServerUi::JRubyFacade.getInstance().getQualifiersWithProperty('deletable')
  end

end

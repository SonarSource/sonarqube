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
class BulkDeletionController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION

  before_filter :admin_required

  def index
    if pending_mass_deletion?
      render :template => 'bulk_deletion/pending_deletions'
      return
    end
        
    init_tab
    params['pageSize'] = 20
    params['qualifiers'] = @selected_tab
    @query_result = Internal.component_api.find(params)
  end

  def ghosts
    if pending_mass_deletion?
      render :template => 'bulk_deletion/pending_deletions'
      return
    end

    @tabs = deletable_qualifiers

    params['pageSize'] = -1
    params['qualifiers'] = @tabs
    @query_result = Internal.component_api.findGhostsProjects(params)

    @ghosts = @query_result.components

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

    if params[:select_all] && params[:select_all] == 'true'
      init_tab
      # Load all matching components to delete when select_all params is present
      params['pageSize'] = -1
      params['qualifiers'] = @selected_tab
      query_result = Internal.component_api.find(params)
      resource_to_delete = query_result.components.map {|component| component.id}
    else
      resource_to_delete = params[:resources] || []
      # Used by the ghost deletion
      resource_to_delete = params[:all_resources].split(',') if params[:all_resources] && !params[:all_resources].blank?
    end

    # Ask the resource deletion manager to start the migration
    # => this is an asynchronous AJAX call
    ResourceDeletionManager.instance.delete_resources(resource_to_delete)

    redirect_to :action => :pending_deletions
  end

  def pending_deletions
    deletion_manager = ResourceDeletionManager.instance

    if deletion_manager.currently_deleting_resources? ||
      (!deletion_manager.currently_deleting_resources? && deletion_manager.deletion_failures_occured?)
      # display the same page again and again
      # => implicit render "pending_deletions.html.erb"
    else
      redirect_to :action => 'index', :resource_type => params[:resource_type]
    end
  end

  def dismiss_message
    # It is important to reinit the ResourceDeletionManager so that the deletion screens can be available again
    ResourceDeletionManager.instance.reinit

    redirect_to :action => 'index', :resource_type => params[:resource_type]
  end


  private

  def init_tab
    @tabs = deletable_qualifiers
    @selected_tab = params[:qualifiers]
    @selected_tab = 'TRK' unless @tabs.include?(@selected_tab)
  end
  
  # Tells if a mass deletion is happening or if it has finished with errors
  def pending_mass_deletion?
    deletion_manager = ResourceDeletionManager.instance
    deletion_manager.currently_deleting_resources? || (!deletion_manager.currently_deleting_resources? && deletion_manager.deletion_failures_occured?)
  end
  
  def deletable_qualifiers
    Java::OrgSonarServerUi::JRubyFacade.getInstance().getQualifiersWithProperty('deletable')
  end

end

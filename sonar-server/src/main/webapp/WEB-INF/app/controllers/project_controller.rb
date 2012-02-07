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
class ProjectController < ApplicationController
  verify :method => :post, :only => [:set_links, :set_exclusions, :delete_exclusions], :redirect_to => {:action => :index}
  verify :method => :delete, :only => [:delete], :redirect_to => {:action => :index}

  SECTION=Navigation::SECTION_RESOURCE

  def index
    redirect_to :overwrite_params => {:controller => :dashboard, :action => 'index'}
  end

  def deletion
    @project=Project.by_key(params[:id])
    not_found("Project not found") unless @project
    access_denied unless is_admin?(@project)

    @snapshot=@project.last_snapshot
    if !@project.project?
      redirect_to :action => 'index', :id => params[:id]
    end
  end

  def delete
    if params[:id]
      @project = Project.by_key(params[:id])
      if @project && @project.project? && is_admin?(@project)
        Project.delete_project(@project)
      end
    end
    redirect_to_default
  end

  def history
    @project=Project.by_key(params[:id])
    not_found("Project not found") unless @project
    access_denied unless is_admin?(@project)

    if !(@project.project? || @project.view? || @project.subview?)
      redirect_to :action => 'index', :id => params[:id]
    end

    @snapshot=@project.last_snapshot
    @snapshots = Snapshot.find(:all, :conditions => ["status='P' AND project_id=?", @project.id],
                               :include => 'events', :order => 'snapshots.created_at DESC')
  end

  def delete_snapshot_history
    @project=Project.by_key(params[:id])
    not_found("Project not found") unless @project
    access_denied unless is_admin?(@project)

    sid = params[:snapshot_id]
    if sid
      Snapshot.update_all("status='U'", ["id=? or root_snapshot_id=(?)", sid.to_i, sid.to_i])
      flash[:notice] = message('project_history.snapshot_deleted')
    end

    redirect_to :action => 'history', :id => @project.id
  end

  def links
    @project=Project.by_key(params[:id])
    not_found("Project not found") unless @project
    access_denied unless is_admin?(@project)

    @snapshot=@project.last_snapshot
    if !@project.project?
      redirect_to :action => 'index', :id => params[:id]
    end
  end

  def set_links
    project = Project.by_key(params[:project_id])
    not_found("Project not found") unless project
    access_denied unless is_admin?(project)

    project.links.clear

    params.each_pair do |param_key, value|
      if (param_key.starts_with?('name_'))
        id = param_key[5..-1]
        name=value
        url=params["url_#{id}"]
        key=params["key_#{id}"]
        if key.blank?
          key=ProjectLink.name_to_key(name)
        end
        unless key.blank? || name.blank? || url.blank?
          project.links.create(:href => url, :name => name, :link_type => key)
        end
      end
    end
    project.save!

    flash[:notice] = 'Links updated.'
    redirect_to :action => 'links', :id => project.id
  end


  def settings
    @project=Project.by_key(params[:id])
    not_found("Project not found") unless @project
    access_denied unless is_admin?(@project)

    @snapshot=@project.last_snapshot
    if !@project.project? && !@project.module?
      redirect_to :action => 'index', :id => params[:id]
    end

    @category=params[:category] ||= 'general'
    @properties_per_category={}
    definitions = java_facade.getPropertyDefinitions()
    properties = definitions.getProperties().select { |property| (@project.module? && property.module()) || (@project.project? && property.project()) }
    properties.each do |property|
      category = definitions.getCategory(property.key())
      @properties_per_category[category]||=[]
      @properties_per_category[category]<<property
    end
  end


  def events
    @categories = EventCategory.categories(true)
    @snapshot = Snapshot.find(params[:id])
    @category = params[:category]
      
    conditions = "resource_id=:resource_id"
    values = {:resource_id => @snapshot.project_id}
    unless @category.blank?
      conditions << " AND category=:category"
      values[:category] = @category
    end
    # in order to not display events linked to deleted snapshot, we build the SQL request with 'NOT IN' as most of the time, there won't be unprocessed snapshots
    snapshots_to_be_deleted = Snapshot.find(:all, :conditions => ["status='U' AND project_id=?", @snapshot.project_id])
    unless snapshots_to_be_deleted.empty?
      conditions << " AND snapshot_id NOT IN (:sids)"
      values[:sids] = snapshots_to_be_deleted.map {|s| s.id}
    end

    category_names=@categories.map { |cat| cat.name }
    @events=Event.find(:all, :conditions => [conditions, values], :order => 'event_date desc').select do |event|
      category_names.include?(event.category)
    end
    render :action => 'events', :layout => !request.xhr?
  end


  def exclusions
    @project=Project.by_key(params[:id])
    not_found("Project not found") unless @project
    access_denied unless is_admin?(@project)

    @snapshot=@project.last_snapshot
    if !@project.project? && !@project.module?
      redirect_to :action => 'index', :id => params[:id]
    end
  end

  def set_exclusions
    @project = Project.find(params[:id])
    not_found("Project not found") unless @project
    access_denied unless is_admin?(@project)

    patterns=params['patterns'].reject { |p| p.blank? }.uniq
    if patterns.empty?
      Property.clear('sonar.exclusions', @project.id)
    else
      # Trim spaces in patterns before merging into one String - see http://jira.codehaus.org/browse/SONAR-2261
      Property.set('sonar.exclusions', patterns.collect { |x| x.strip }.join(','), @project.id)
    end
    flash[:notice]='Filters added'
    redirect_to :action => 'exclusions', :id => @project.id
  end

  def delete_exclusions
    @project = Project.find(params[:id])
    not_found("Project not found") unless @project
    access_denied unless is_admin?(@project)

    Property.clear('sonar.exclusions', @project.id)
    flash[:notice]='Filters deleted'
    redirect_to :action => 'exclusions', :id => @project.id
  end

  def update_version
    snapshot=Snapshot.find(params[:sid])
    not_found("Snapshot not found") unless snapshot
    access_denied unless is_admin?(snapshot)

    unless params[:version_name].blank?
      if Event.already_exists(snapshot.id, params[:version_name], EventCategory::KEY_VERSION)
        flash[:error] = message('project_history.version_already_exists', :params => params[:version_name])
      else
        snapshots = find_project_snapshots(snapshot.id)
        # We update all the related snapshots to have a version attribute in sync with the new name
        snapshots.each do |snapshot|
          snapshot.version = params[:version_name]
          snapshot.save!
        end
        # And then we update/create the event on each snapshot
        if snapshot.event(EventCategory::KEY_VERSION)
          # This is an update: we update all the related events
          Event.update_all({:name => params[:version_name]},
                           ["category = ? AND snapshot_id IN (?)", EventCategory::KEY_VERSION, snapshots.map { |s| s.id }])
          flash[:notice] = message('project_history.version_updated', :params => params[:version_name])
        else
          # We create an event for every concerned snapshot
          snapshots.each do |snapshot|
            event = Event.create!(:name => params[:version_name], :snapshot => snapshot,
                                  :resource_id => snapshot.project_id, :category => EventCategory::KEY_VERSION,
                                  :event_date => snapshot.created_at)
          end
          flash[:notice] = message('project_history.version_created', :params => params[:version_name])
        end
      end
    end

    redirect_to :action => 'history', :id => snapshot.root_project_id
  end

  def delete_version
    snapshot=Snapshot.find(params[:sid])
    not_found("Snapshot not found") unless snapshot
    access_denied unless is_admin?(snapshot)

    event = snapshot.event(EventCategory::KEY_VERSION)
    old_version_name = event.name
    events = find_events(event)
    Event.delete(events.map { |e| e.id })

    flash[:notice] = message('project_history.version_removed', :params => old_version_name)
    redirect_to :action => 'history', :id => snapshot.root_project_id
  end

  def create_event
    snapshot=Snapshot.find(params[:sid])
    not_found("Snapshot not found") unless snapshot
    access_denied unless is_admin?(snapshot)

    if Event.already_exists(snapshot.id, params[:event_name], EventCategory::KEY_OTHER)
      flash[:error] = message('project_history.event_already_exists', :params => params[:event_name])
    else
      snapshots = find_project_snapshots(snapshot.id)
      snapshots.each do |s|
      e = Event.new({:name => params[:event_name], 
                     :category => EventCategory::KEY_OTHER,
                     :snapshot => s,
                     :resource_id => s.project_id,
                     :event_date => s.created_at})
        e.save!
      end
      flash[:notice] = message('project_history.event_created', :params => params[:event_name])
    end

    redirect_to :action => 'history', :id => snapshot.project_id
  end

  def update_event
    event = Event.find(params[:id])
    not_found("Event not found") unless event
    access_denied unless is_admin?(event.resource)

    if Event.already_exists(event.snapshot_id, params[:event_name], EventCategory::KEY_OTHER)
      flash[:error] = message('project_history.event_already_exists', :params => event.name)
    else
      events = find_events(event)
      events.each do |e|
        e.name = params[:event_name]
        e.save!
      end
      flash[:notice] = message('project_history.event_updated')
    end

    redirect_to :action => 'history', :id => event.resource_id
  end

  def delete_event
    event = Event.find(params[:id])
    not_found("Event not found") unless event
    access_denied unless is_admin?(event.resource)

    name = event.name
    resource_id = event.resource_id
    events = find_events(event)
    Event.delete(events.map { |e| e.id })

    flash[:notice] = message('project_history.event_deleted', :params => name)
    redirect_to :action => 'history', :id => resource_id
  end

  protected

  def find_project_snapshots(root_snapshot_id)
    snapshots = Snapshot.find(:all, :include => 'events', :conditions => ["(root_snapshot_id = ? OR id = ?) AND scope = 'PRJ'", root_snapshot_id, root_snapshot_id])
  end

  # Returns all an array that contains the given event + all the events that are the same, but which are attached on the submodules
  def find_events(event)
    events = []
    name = event.name
    category = event.category
    description = event.description
    snapshots = find_project_snapshots(event.snapshot_id)
    snapshots.each do |snapshot|
      snapshot.events.reject { |e| e.name!=name || e.category!=category }.each do |event|
        events << event
      end
    end
    events
  end

  def redirect_to_default
    redirect_to home_path
  end

end
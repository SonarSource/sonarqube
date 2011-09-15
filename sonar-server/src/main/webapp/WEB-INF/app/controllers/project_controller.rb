#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2011 SonarSource
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
  verify :method => :post, :only => [  :set_links, :set_exclusions, :delete_exclusions ], :redirect_to => { :action => :index }
  verify :method => :delete, :only => [ :delete ], :redirect_to => { :action => :index }

  SECTION=Navigation::SECTION_RESOURCE
  
  def index
    redirect_to :overwrite_params => {:controller => :dashboard, :action => 'index'} 
  end

  def deletion
    @project=Project.by_key(params[:id])
    return access_denied unless is_admin?(@project)

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
    return access_denied unless is_admin?(@project)

    if !(@project.project? || @project.view? || @project.subview?)
      redirect_to :action => 'index', :id => params[:id]
    end
    
    @snapshot=@project.last_snapshot
    @snapshots = Snapshot.find(:all, :conditions => ["status='P' AND project_id=?", @project.id], 
                               :include => 'events', :order => 'snapshots.created_at DESC')
  end

  def delete_snapshot_history
    project=Project.by_key(params[:id])
    return access_denied unless is_admin?(@project)
    
    sid = params[:snapshot_id]
    if sid
      Snapshot.update_all("status='U'", ["id=? or root_snapshot_id=(?)", sid, sid])
      flash[:notice] = message('project_history.snapshot_deleted')
    end
    
    redirect_to :action => 'history', :id => project.id
  end

  def links
    @project=Project.by_key(params[:id])
    return access_denied unless is_admin?(@project)

    @snapshot=@project.last_snapshot
    if !@project.project?
      redirect_to :action => 'index', :id => params[:id]
    end
  end

  def set_links
    project = Project.by_key(params[:project_id])
    return access_denied unless is_admin?(project)

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
    return access_denied unless is_admin?(@project)

    @snapshot=@project.last_snapshot
    if !@project.project? && !@project.module?
      redirect_to :action => 'index', :id => params[:id]
    end

    @category=params[:category] ||= 'general'
    @properties_per_category={}
    java_facade.getPluginsMetadata().each do |plugin|
      properties=java_facade.getPluginProperties(plugin).select { |property|
        (@project.module? && property.module()) || (@project.project? && property.project())
      }
      properties.each do |property|
        category = (property.category().present? ? property.category() : plugin.name())
        @properties_per_category[category]||=[]
        @properties_per_category[category]<<property
      end
    end
  end


  def events
    @categories=EventCategory.categories(true)
    @snapshot=Snapshot.find(params[:id])
    @category=params[:category]
    conditions={:resource_id => @snapshot.project_id}
    conditions[:category]=@category unless @category.blank?

    category_names=@categories.map{|cat| cat.name}
    @events=Event.find(:all, :conditions => conditions, :order => 'event_date desc').select do |event|
      category_names.include?(event.category)
    end
    render :action => 'events', :layout => ! request.xhr?
  end


  def exclusions
    @project=Project.by_key(params[:id])
    return access_denied unless is_admin?(@project)
  
    @snapshot=@project.last_snapshot
    if !@project.project? && !@project.module?
      redirect_to :action => 'index', :id => params[:id]
    end
  end

  def set_exclusions
    @project = Project.find(params[:id])
    return access_denied unless is_admin?(@project)

    patterns=params['patterns'].reject{|p| p.blank?}.uniq
    if patterns.empty?
      Property.clear('sonar.exclusions', @project.id)
    else
      # Trim spaces in patterns before merging into one String - see http://jira.codehaus.org/browse/SONAR-2261
      Property.set('sonar.exclusions', patterns.collect{|x| x.strip}.join(','), @project.id)
    end
    flash[:notice]='Filters added'
    redirect_to :action => 'exclusions', :id => @project.id
  end

  def delete_exclusions
    @project = Project.find(params[:id])
    return access_denied unless is_admin?(@project)
    
    Property.clear('sonar.exclusions', @project.id)
    flash[:notice]='Filters deleted'
    redirect_to :action => 'exclusions', :id => @project.id
  end

  protected

  def redirect_to_default
    redirect_to home_path
  end

end
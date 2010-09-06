#
# Sonar, entreprise quality control tool.
# Copyright (C) 2009 SonarSource SA
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
  verify :method => :post, :only => [  :set_links, :add_review, :set_exclusions, :delete_exclusions ], :redirect_to => { :action => :index }
  verify :method => :delete, :only => [ :delete, :delete_review ], :redirect_to => { :action => :index }

  SECTION=Navigation::SECTION_RESOURCE
  
  def index
    @project=Project.by_key(params[:id])
    unless @project
      redirect_to_default
      return
    end
    return access_denied unless has_role?(:user, @project)
    @snapshot = @project.last_snapshot


    load_widgets

  rescue ActiveRecord::RecordNotFound
    redirect_to_default
  end

  def show_reviews
    @snapshot = Snapshot.find(params[:sid].to_i)
    render :partial => 'dashboard_reviews'
  end

  def edit_review
    @snapshot = Snapshot.find(params[:sid].to_i)
    return access_denied unless has_role?(:admin, @snapshot)

    @review = ProjectMeasure.new(:measure_date => @snapshot.created_at, :value => 0)
    @review_types = Metric.review_types
    render :partial => 'dashboard_edit_review'
  end

  def add_review
    @snapshot=Snapshot.find(params[:sid].to_i)
    return access_denied unless has_role?(:admin, @snapshot)

    measure = ProjectMeasure.new(params[:review])
    measure.project = @snapshot.project

    if measure.metric.nil?
      flash[:error] = 'Please select a metric'
      redirect_to :action => 'index', :id => measure.project_id
      return
    end

    if measure.measure_date <= @snapshot.created_at.to_date
      if measure.metric.val_type==Metric::VALUE_TYPE_STRING
        measure.text_value=params['review']['value']
        measure.value=0
      end
      measure.url=params[:url] if params[:url]
      measure.description=params[:description] if params[:description]
      begin
        measure.save!
        java_facade.registerAsyncMeasure(measure.id.to_i)
        flash[:notice] = 'Measure added'
      rescue
        flash[:error] = measure.errors.full_messages.join("<br/>")
      end

    else
      flash[:error] = "The date should not be after #{l(@snapshot.created_at.to_date)}"
    end

    if request.xhr?
      render :update do |page|
        page.redirect_to :action => 'index', :id => measure.project_id
      end
    else
      redirect_to :action => 'index', :id => measure.project_id
    end
  end

  def delete_review
    measure = ProjectMeasure.find(params[:id].to_i)
    if measure && measure.review?
      return access_denied unless has_role?(:admin, measure.project)

      java_facade.deleteAsyncMeasure(measure.id.to_i)
      redirect_to :action => 'index', :id => measure.project_id
    else
      redirect_to_default
    end
  end

  def delete
    if params[:id]
      @project = Project.by_key(params[:id])
      if @project && is_admin?(@project)
        Snapshot.update_all(['islast=?', false], ['(root_project_id=? OR project_id=?) AND islast=?', @project.id, @project.id, true])
        Project.delete_all(['id=? OR root_id=?', @project.id, @project.id])
      end
    end
    redirect_to_default
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
    redirect_to :action => 'settings', :id => project.id
  end

  def settings
    @project=Project.by_key(params[:id])
    return access_denied unless is_admin?(@project)

    @snapshot=@project.last_snapshot
    if !@project.project? && !@project.module?
      redirect_to :action => 'index', :id => params[:id]
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


  def set_exclusions
    @project = Project.find(params[:id])
    return access_denied unless is_admin?(@project)

    patterns=params['patterns'].reject{|p| p.blank?}.uniq
    if patterns.empty?
      Property.clear('sonar.exclusions', @project.id)
    else
      Property.set('sonar.exclusions', patterns.join(','), @project.id)
    end
    flash[:notice]='Filters added'
    redirect_to :action => 'settings', :id => @project.id
  end

  def delete_exclusions
    @project = Project.find(params[:id])
    return access_denied unless is_admin?(@project)
    
    Property.clear('sonar.exclusions', @project.id)
    flash[:notice]='Filters deleted'
    redirect_to :action => 'settings', :id => @project.id
  end

  protected

  def redirect_to_default
    redirect_to home_path
  end

  def load_widgets
    @widgets = java_facade.getWidgets(@project.scope, @project.qualifier, @project.language)
    @widgets=@widgets.select do |widget|
      authorized=widget.getUserRoles().size==0
      unless authorized
        widget.getUserRoles().each do |role|
          authorized=(role=='user') || (role=='viewer') || has_role?(role, @project)
          break if authorized
        end
      end
      authorized
    end
  end
end
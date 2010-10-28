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
class DashboardController < ApplicationController

  SECTION=Navigation::SECTION_RESOURCE

  verify :method => :post, :only => [:set_layout, :add_widget, :set_dashboard], :redirect_to => {:action => :index}
  before_filter :login_required, :except => [:index]

  def index
    # TODO display error page if no dashboard or no resource
    load_dashboard()
    load_resource()
    load_authorized_widget_definitions()
  end

  def configure
    # TODO display error page if no dashboard or no resource
    load_dashboard()
    load_resource()
    load_widget_definitions()
  end

  def edit_layout
    load_dashboard()
    load_resource()
  end

  def set_layout
    dashboard=Dashboard.find(params[:id].to_i)
    if dashboard.editable_by?(current_user)
      dashboard.column_layout=params[:layout]
      if dashboard.save
        columns=dashboard.column_layout.split('-')
        dashboard.widgets.find(:all, :conditions => ["column_index > ?",columns.size()]).each do |widget|
          widget.column_index=columns.size()
          widget.save
        end
      end
    end
    redirect_to :action => 'index', :id => dashboard.id, :resource => params[:resource]
  end

  def set_dashboard
    load_dashboard()

    dashboardstate=params[:dashboardstate]

    columns=dashboardstate.split(";")
    all_ids=[]
    columns.each_with_index do |col, index|
      ids=col.split(",")
      ids.each_with_index do |id, order|
        widget=@dashboard.widgets.to_a.find { |i| i.id==id.to_i() }
        if widget
          widget.column_index=index+1
          widget.order_index=order+1
          widget.save!
          all_ids<<widget.id
        end
      end 
    end
    @dashboard.widgets.reject{|w| all_ids.include?(w.id)}.each do |w|
      w.destroy
    end
    render :json => {:status => 'ok'}
  end

  def add_widget
    dashboard=Dashboard.find(params[:id].to_i)
    widget_id=nil
    if dashboard.editable_by?(current_user)
      definition=java_facade.getWidget(params[:widget])
      if definition
        new_widget=dashboard.widgets.create(:widget_key => definition.getId(),
                                           :name => definition.getTitle(),
                                           :column_index => dashboard.number_of_columns,
                                           :order_index => dashboard.column_size(dashboard.number_of_columns) + 1,
                                           :state => Widget::STATE_ACTIVE)
        widget_id=new_widget.id
      end
    end
    redirect_to :action => 'configure', :id => dashboard.id, :resource => params[:resource], :highlight => widget_id 
  end

  private

  def load_dashboard
    @active=nil
    if logged_in?
      if params[:id]
        @active=ActiveDashboard.find(:first, :include => 'dashboard', :conditions => ['active_dashboards.dashboard_id=? AND active_dashboards.user_id=?', params[:id].to_i, current_user.id])
      elsif params[:name]
        @active=ActiveDashboard.find(:first, :include => 'dashboard', :conditions => ['dashboards.name=? AND active_dashboards.user_id=?', params[:name], current_user.id])
      else
        @active=ActiveDashboard.find(:first, :include => 'dashboard', :conditions => ['active_dashboards.user_id=?', current_user.id], :order => 'order_index ASC')
      end
    end

    if @active.nil?
      # anonymous or not found in user dashboards
      if params[:id]
        @active=ActiveDashboard.find(:first, :include => 'dashboard', :conditions => ['active_dashboards.dashboard_id=? AND active_dashboards.user_id IS NULL', params[:id].to_i])
      elsif params[:name]
        @active=ActiveDashboard.find(:first, :include => 'dashboard', :conditions => ['dashboards.name=? AND active_dashboards.user_id IS NULL', params[:name]])
      else
        @active=ActiveDashboard.find(:first, :include => 'dashboard', :conditions => ['active_dashboards.user_id IS NULL'], :order => 'order_index ASC')
      end
    end
    @dashboard=(@active ? @active.dashboard : nil)
  end

  def load_resource
    @resource=Project.by_key(params[:resource])
    if @resource.nil?
      # TODO display error page
      redirect_to home_path
      return false
    end
    return access_denied unless has_role?(:user, @resource)
    @snapshot = @resource.last_snapshot
    @project=@resource  # variable name used in old widgets
  end

  def load_authorized_widget_definitions()
    @widget_definitions = java_facade.getWidgets(@resource.scope, @resource.qualifier, @resource.language)
    @widget_definitions=@widget_definitions.select do |widget|
      authorized=widget.getUserRoles().size==0
      unless authorized
        widget.getUserRoles().each do |role|
          authorized=(role=='user') || (role=='viewer') || has_role?(role, @resource)
          break if authorized
        end
      end
      authorized
    end
  end

  def load_widget_definitions()
    @widget_definitions = java_facade.getWidgets()
  end
end
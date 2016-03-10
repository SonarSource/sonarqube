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
include ERB::Util
class DashboardController < ApplicationController

  SECTION=Navigation::SECTION_RESOURCE

  before_filter :login_required, :except => [:index]

  def index
    load_resource()
      if !@resource || @resource.display_dashboard?
        if params[:id]
          unless @resource
            return project_not_found
          end
          unless @snapshot
            return project_not_analyzed
          end
        end

        # redirect to the project overview
        if params[:id] && !params[:did] && !params[:name] && @resource.qualifier != 'DEV'
          # if governance plugin is installed and we are opening a view
          if Project.root_qualifiers.include?('VW') && (@resource.qualifier == 'VW' || @resource.qualifier == 'SVW')
            return redirect_to(url_for({:controller => 'governance'}) + '?id=' + url_encode(params[:id]))
          else
            return redirect_to(url_for({:controller => 'overview'}) + '?id=' + url_encode(params[:id]))
          end
        end

        load_dashboard()
        load_authorized_widget_definitions()
      else
        if !@resource || !@snapshot
          redirect_if_bad_component()
        else
          # display the layout of the parent without the sidebar, usually the directory, but display the file viewers
          @hide_sidebar = true
          @file = @resource
          @project = @snapshot.parent.project
          @metric=params[:metric]
          render :action => 'no_dashboard'
      end
    end
  end

  def configure
    load_resource()
    redirect_if_bad_component()
    load_dashboard()

    @category=params[:category]
    load_widget_definitions(@category)
  end

  def set_layout
    verify_post_request
    dashboard=Dashboard.find(params[:did])
    if dashboard.editable_by?(current_user)
      dashboard.column_layout=params[:layout]
      dashboard.save!
      columns=dashboard.column_layout.split('-')
      dashboard.widgets.find(:all, :conditions => ["column_index > ?", columns.size()]).each do |widget|
        widget.column_index=columns.size()
        widget.save
      end
    end
    redirect_to :action => 'configure', :did => dashboard.id, :id => params[:id]
  end

  def set_dashboard
    verify_post_request
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
          widget.row_index=order+1
          widget.save!
          all_ids<<widget.id
        end
      end
    end
    @dashboard.widgets.reject { |w| all_ids.include?(w.id) }.each do |w|
      w.destroy
    end
    render :json => {:status => 'ok'}
  end

  def add_widget
    verify_post_request
    dashboard=Dashboard.find(params[:did])
    widget_id=nil
    if dashboard.editable_by?(current_user)
      definition=java_facade.getWidget(params[:widget])
      if definition
        first_column_widgets=dashboard.widgets.select { |w| w.column_index==1 }.sort_by { |w| w.row_index }
        new_widget=dashboard.widgets.create(:widget_key => definition.getId(),
                                            :name => definition.getTitle(),
                                            :column_index => 1,
                                            :row_index => 1,
                                            :configured => !(definition.hasRequiredProperties() || (dashboard.global && !definition.isGlobal)))
        widget_id=new_widget.id
        first_column_widgets.each_with_index do |w, index|
          w.row_index=index+2
          w.save
        end
      end
    end
    redirect_to :action => 'configure', :did => dashboard.id, :id => params[:id], :highlight => widget_id, :category => params[:category]
  end

  def save_widget
    verify_post_request
    widget=Widget.find(params[:wid])
    #TODO check owner of dashboard
    Widget.transaction do
      widget.properties.clear
      widget.java_definition.getWidgetProperties().each do |java_property|
        value=params[java_property.key()] || java_property.defaultValue()
        if value && !value.empty?
          prop = widget.properties.build(:kee => java_property.key, :text_value => value)
          prop.save!
        end
      end
      widget.resource_id=Project.by_key(params[:resource_id]).id if params[:resource_id].present?
      widget.configured=true
      widget.save!
      render :update do |page|
        page.redirect_to(url_for(:action => 'configure', :did => widget.dashboard_id, :id => params[:id]))
      end
    end
  end

  def widget_definitions
    @category=params[:category]
    load_resource()
    # redirect_if_bad_component()
    load_dashboard()
    load_widget_definitions(@category)
    render :partial => 'widget_definitions', :locals => {:category => @category}
  end

  private

  def load_dashboard
    active=nil
    @dashboard=nil

    if logged_in?
      if params[:did]
        @dashboard=Dashboard.first(:conditions => ['id=? AND user_id=?', params[:did].to_i, current_user.id])
      elsif params[:name]
        @dashboard=Dashboard.first(:conditions => ['name=? AND user_id=?', params[:name], current_user.id])
      elsif params[:id]
        active=ActiveDashboard.user_dashboards(current_user, false).first
      else
        active=ActiveDashboard.user_dashboards(current_user, true).first
      end
    end

    unless active or @dashboard
      # anonymous or not found in user dashboards
      if params[:did]
        @dashboard=Dashboard.first(:conditions => ['id=? AND shared=?', params[:did].to_i, true])
      elsif params[:name]
        @dashboard=Dashboard.first(:conditions => ['name=? AND shared=?', params[:name], true])
      elsif params[:id]
        active=ActiveDashboard.user_dashboards(nil, false).first
      else
        active=ActiveDashboard.user_dashboards(nil, true).first
      end
    end

    unless @dashboard
      @dashboard=(active && active.dashboard)
    end

    not_found('dashboard') unless @dashboard

    @dashboard_configuration=Api::DashboardConfiguration.new(@dashboard, :period_index => params[:period], :snapshot => @snapshot) if @dashboard && @snapshot
  end

  def load_resource
    if params[:id]
      @resource = Project.by_key(params[:id])
      return unless @resource
      @resource=@resource.permanent_resource

      @snapshot=@resource.last_snapshot
      return unless @snapshot

      access_denied unless has_role?(:user, @resource)

      @project=@resource # for backward compatibility with old widgets
    end
  end

  def redirect_if_bad_component
    if params[:id]
      unless @resource
        return project_not_found
      end
      unless @snapshot
        project_not_analyzed
      end
    end
  end

  def project_not_found
    flash[:error] = message('dashboard.project_not_found')
    redirect_to :action => :index
  end

  def project_not_analyzed
    render :action => 'empty'
  end

  def load_authorized_widget_definitions
    @authorized_widget_definitions=java_facade.getWidgets().select do |widget|
      roles = widget.getUserRoles()
      roles.empty? || roles.any? { |role| (role=='user') || (role=='viewer') || has_role?(role, @resource) }
    end
  end

  def load_widget_definitions(filter_on_category)
    @widget_definitions=java_facade.getWidgets().to_a.sort {|w1,w2| widgetL10nName(w1) <=> widgetL10nName(w2)}

    @widget_categories=@widget_definitions.map(&:getWidgetCategories).to_a.flatten.uniq.sort
    unless filter_on_category.blank?
      @widget_definitions=@widget_definitions.select { |definition| definition.getWidgetCategories().to_a.include?(filter_on_category) }
    end
  end

  def widgetL10nName(widget)
    Api::Utils.message('widget.' + widget.id + '.name')
  end
end

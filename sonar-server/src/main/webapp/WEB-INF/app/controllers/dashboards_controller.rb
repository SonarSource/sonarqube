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
class DashboardsController < ApplicationController

  SECTION=Navigation::SECTION_RESOURCE

  before_filter :login_required

  def index
    @global = !params[:resource]

    @actives=ActiveDashboard.user_dashboards(current_user, @global)
    @shared_dashboards=Dashboard.all(:conditions => ['(shared=? or user_id=?) and is_global=?', true, current_user.id, @global])
    active_ids=@actives.map(&:dashboard_id)
    @shared_dashboards.reject! { |d| active_ids.include?(d.id) }
    @shared_dashboards=Api::Utils.insensitive_sort(@shared_dashboards, &:name)

    if params[:resource]
      @resource=Project.by_key(params[:resource])
      if @resource.nil?
        # TODO display error page
        redirect_to home_path
        return false
      end
      access_denied unless has_role?(:user, @resource)
      @snapshot = @resource.last_snapshot
      @project=@resource # variable name used in old widgets
    end
  end

  def create_form
    @global = !params[:resource]
    @dashboard = Dashboard.new
    render :partial => 'create_form', :resource => params[:resource]
  end

  def create
    verify_post_request
    @global = !params[:resource]
    @dashboard = Dashboard.new()
    @dashboard.user_id = current_user.id
    load_dashboard_from_params(@dashboard)

    active_dashboard = current_user.active_dashboards.to_a.find { |ad| ad.name==@dashboard.name }
    if active_dashboard
      @dashboard.errors.add_to_base(Api::Utils.message('dashboard.error_create_existing_name'))
      render :partial => 'dashboards/create_form', :status => 400, :resource => params[:resource]
    elsif @dashboard.save
      add_default_dashboards_if_first_user_dashboard(@dashboard.global?)
      last_index=current_user.active_dashboards.max_by(&:order_index).order_index
      current_user.active_dashboards.create(:dashboard => @dashboard, :user => current_user, :order_index => (last_index+1))
      render :text => CGI.escapeHTML(params[:resource]), :highlight => @dashboard.id, :status => 200
    else
      render :partial => 'dashboards/create_form', :status => 400, :resource => params[:resource]
    end
  end

  def edit_form
    @dashboard = Dashboard.find(params[:id])
    if @dashboard.editable_by?(current_user)
      render :partial => 'edit_form', :resource => params[:resource]
    else
      access_denied
    end
  end

  def update
    verify_post_request
    @dashboard = Dashboard.find(params[:id])
    dashboard_owner = @dashboard.user
    if @dashboard.editable_by?(current_user)
      load_dashboard_from_params(@dashboard)
      if @dashboard.save

        # SONAR-4979 If the dashboard is no more shared, current user has to unfollow it if he was following it
        unless @dashboard.shared
          active = current_user.active_dashboards.to_a.find { |a| (a.user_id == dashboard_owner.id) && (a.dashboard_id == @dashboard.id)}
          active.destroy if active
        end

        render :text => CGI.escapeHTML(params[:resource]), :status => 200
      else
        @dashboard.user = dashboard_owner
        render :partial => 'dashboards/edit_form', :status => 400, :resource => params[:resource]
      end
    else
      access_denied
    end
  end

  def delete_form
    @dashboard = Dashboard.find(params[:id])
    if @dashboard.editable_by?(current_user)
      render :partial => 'delete_form', :resource => params[:resource]
    else
      access_denied
    end
  end

  def delete
    verify_post_request
    @dashboard=Dashboard.find(params[:id])

    access_denied unless @dashboard.editable_by?(current_user)

    if @dashboard.destroy
      flash[:warning]=Api::Utils.message('dashboard.default_restored') if ActiveDashboard.count(:conditions => {:user_id => current_user.id})==0
      render :text => CGI.escapeHTML(params[:resource]), :status => 200
    else
      @dashboard.errors.add(message('dashboard.error_delete_default'), ' ')
      render :partial => 'dashboards/delete_form', :status => 400, :resource => params[:resource]
    end
  end

  def down
    verify_post_request
    position(+1)
  end

  def up
    verify_post_request
    position(-1)
  end

  def follow
    verify_post_request
    dashboard=Dashboard.find(params[:id])

    add_default_dashboards_if_first_user_dashboard(dashboard.global?)
    active_dashboard = current_user.active_dashboards.to_a.find { |ad| ad.name==dashboard.name }
    if active_dashboard
      flash[:error]=Api::Utils.message('dashboard.error_follow_existing_name')
    else
      last_active_dashboard=current_user.active_dashboards.max_by(&:order_index)
      current_user.active_dashboards.create(:dashboard => dashboard, :user => current_user, :order_index => (last_active_dashboard ? last_active_dashboard.order_index+1 : 1))
    end

    redirect_to :action => 'index', :resource => params[:resource]
  end

  def unfollow
    verify_post_request
    dashboard=Dashboard.find(params[:id])

    add_default_dashboards_if_first_user_dashboard(dashboard.global?)
    ActiveDashboard.destroy_all(:user_id => current_user.id, :dashboard_id => params[:id].to_i)

    if ActiveDashboard.count(:conditions => {:user_id => current_user.id})==0
      flash[:notice]=Api::Utils.message('dashboard.default_restored')
    end

    redirect_to :action => 'index', :resource => params[:resource]
  end


  private

  def position(offset)
    dashboard=Dashboard.find(params[:id])

    add_default_dashboards_if_first_user_dashboard(dashboard.global?)
    actives=current_user.active_dashboards.select { |a| a.global? == dashboard.global? }.sort_by(&:order_index)

    index = actives.index { |a| a.dashboard_id == dashboard.id }
    if index
      actives[index], actives[index + offset] = actives[index + offset], actives[index]

      actives.each_with_index do |a, i|
        a.order_index=i+1
        a.save
      end
    end

    redirect_to :action => 'index', :resource => params[:resource]
  end

  def load_dashboard_from_params(dashboard)
    dashboard.name = params[:name]
    dashboard.description = params[:description]
    dashboard.is_global = params[:global].present?
    dashboard.shared = params[:shared].present? && has_role?(:shareDashboard)
    dashboard.column_layout = Dashboard::DEFAULT_LAYOUT if !dashboard.column_layout
    dashboard.user = User.find_active_by_login(params[:owner]) unless params[:owner].nil?
  end

  def add_default_dashboards_if_first_user_dashboard(global)
    unless current_user.active_dashboards.any? { |a| a.global? == global }
      ActiveDashboard.default_dashboards.select { |a| a.global? == global }.each do |default_active|
        current_user.active_dashboards.create(:dashboard => default_active.dashboard, :user => current_user, :order_index => default_active.order_index)
      end
    end
  end

end

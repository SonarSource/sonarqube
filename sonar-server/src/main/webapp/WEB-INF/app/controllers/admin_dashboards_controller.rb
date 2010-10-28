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
class AdminDashboardsController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION

  verify :method => :post, :only => [:up, :down, :remove, :add], :redirect_to => {:action => :index}
  before_filter :admin_required
  before_filter :load_active_dashboards

  def index
    @default_dashboards=::Dashboard.find(:all, :conditions => {:shared => true})
    ids=@actives.map{|af| af.dashboard_id}
    if !ids.nil? && !ids.empty?
      @default_dashboards=@default_dashboards.reject!{|f| ids.include?(f.id) }
    end 
  end

  def up
    dashboard_index=-1
    dashboard=nil
    @actives.each_index do |index|
      if @actives[index].id==params[:id].to_i
        dashboard_index=index
        dashboard=@actives[index]
      end
    end
    if dashboard && dashboard_index>0
      @actives[dashboard_index]=@actives[dashboard_index-1]
      @actives[dashboard_index-1]=dashboard

      @actives.each_index do |index|
        @actives[index].order_index=index+1
        @actives[index].save
      end
    end
    redirect_to :action => 'index'
  end

  def down
    dashboard_index=-1
    dashboard=nil
    @actives.each_index do |index|
      if @actives[index].id==params[:id].to_i
        dashboard_index=index
        dashboard=@actives[index]
      end
    end
    if dashboard && dashboard_index<@actives.size-1
      @actives[dashboard_index]=@actives[dashboard_index+1]
      @actives[dashboard_index+1]=dashboard

      @actives.each_index do |index|
        @actives[index].order_index=index+1
        @actives[index].save
      end
    end
    redirect_to :action => 'index'
  end

  def add
    dashboard=::Dashboard.find(:first, :conditions => ['shared=? and id=?', true, params[:id].to_i()])
    if dashboard
      ActiveDashboard.create(:dashboard => dashboard, :user => nil, :order_index => @actives.size+1)
      flash[:notice]='Default dashboard added.'
    end
    redirect_to :action => 'index'
  end

  def remove
    active=@actives.to_a.find{|af| af.id==params[:id].to_i}
    if active
      active.destroy
      flash[:notice]='Dashboard removed from default dashboards.'
    end
    redirect_to :action => 'index'
  end

  private

  def load_active_dashboards
    @actives=ActiveDashboard.default_active_dashboards
  end
end

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
class AdminDashboardsController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION

  verify :method => :post, :only => [:up, :down, :remove, :add, :delete], :redirect_to => {:action => :index}
  before_filter :admin_required
  before_filter :load_default_dashboards

  def index
    @default_dashboards=::Dashboard.find(:all, :conditions => {:shared => true}).sort { |a, b| a.name.downcase<=>b.name.downcase }
    ids=@actives.map { |af| af.dashboard_id }
    if !ids.nil? && !ids.empty?
      @default_dashboards=@default_dashboards.reject! { |f| ids.include?(f.id) }
    end
  end

  def down
    position(+1)
  end

  def up
    position(-1)
  end

  def add
    dashboard=::Dashboard.find(:first, :conditions => ['shared=? and id=?', true, params[:id].to_i()])
    if dashboard
      ActiveDashboard.create(:dashboard => dashboard, :user => nil, :order_index => (@actives.max_by(&:order_index).order_index+1))
      flash[:notice]='Default dashboard added.'
    end
    redirect_to :action => 'index'
  end

  # Remove dashboard from defaults
  def remove
    if @actives.size<=1
      flash[:error]='At least one dashboard must be defined as default.'
    else
      active=@actives.to_a.find { |af| af.id==params[:id].to_i }
      if active
        active.destroy
        flash[:notice]='Dashboard removed from default dashboards.'
      end
    end
    redirect_to :action => 'index'
  end

  # Completely delete dashboard
  def delete
    dashboard=::Dashboard.find(params[:id])
    bad_request('Bad dashboard') unless dashboard
    bad_request('This dashboard can not be deleted') unless dashboard.provided_programmatically?

    if dashboard.destroy
      flash[:notice]="Dashboard #{dashboard.name(true)} deleted."
    else
      flash[:error]="Can't be deleted as long as it's used as default dashboard."
    end
    redirect_to :action => 'index'
  end

  private

  def load_default_dashboards
    @actives=ActiveDashboard.default_dashboards
  end

  def position(offset)
    to_move = @actives.find { |a| a.id == params[:id].to_i}
    if to_move
      dashboards_same_type=@actives.select { |a| (a.global? == to_move.global?) }.sort_by(&:order_index)

      index = dashboards_same_type.index(to_move)
      dashboards_same_type[index], dashboards_same_type[index + offset] = dashboards_same_type[index + offset], dashboards_same_type[index]

      dashboards_same_type.each_with_index do |a,i|
        a.order_index=i+1
        a.save
      end
    end

    redirect_to :action => 'index'
  end

end

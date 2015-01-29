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
class AdminDashboardsController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION

  before_filter :admin_required
  before_filter :load_default_dashboards

  def index
    ids=@actives.map(&:dashboard_id)
    @shared_dashboards=Dashboard.find(:all, :conditions => {:shared => true}).sort { |a, b| a.name.downcase<=>b.name.downcase }
    @shared_dashboards.reject! { |s| ids.include?(s.id) }
  end

  def down
    verify_post_request
    position(+1)
    redirect_to :action => 'index'
  end

  def up
    verify_post_request
    position(-1)
    redirect_to :action => 'index'
  end

  def add
    verify_post_request
    dashboard=Dashboard.find(params[:id])
    if dashboard and dashboard.shared?
      last_index = @actives.max_by(&:order_index).order_index

      ActiveDashboard.create(:dashboard => dashboard, :order_index => last_index+1)
      flash[:notice]='Default dashboard added.'
    end

    redirect_to :action => 'index'
  end

  def remove
    verify_post_request

    to_be_removed = ActiveDashboard.find(params[:id])
    not_found unless to_be_removed

    remaining_defaults = @actives.select { |a| (a.global? == to_be_removed.global? && a.id != to_be_removed.id) }
    if remaining_defaults.size == 0
      flash[:error]='At least one dashboard must be defined as default.'
    else
      to_be_removed.destroy
      flash[:notice]='Dashboard removed from default dashboards.'
    end

    redirect_to :action => 'index'
  end

  private

  def load_default_dashboards
    @actives=ActiveDashboard.default_dashboards
  end

  def position(offset)
    to_move = @actives.find { |a| a.id == params[:id].to_i }
    if to_move
      dashboards_same_type=@actives.select { |a| (a.global? == to_move.global?) }.sort_by(&:order_index)

      index = dashboards_same_type.index(to_move)
      dashboards_same_type[index], dashboards_same_type[index + offset] = dashboards_same_type[index + offset], dashboards_same_type[index]

      dashboards_same_type.each_with_index do |a, i|
        a.order_index=i+1
        a.save
      end
    end
  end

end

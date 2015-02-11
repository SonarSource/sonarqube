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
class DrilldownController < ApplicationController
  before_filter :init_resource_for_user_role

  helper ProjectHelper, DashboardHelper, IssuesHelper

  SECTION=Navigation::SECTION_RESOURCE

  def measures
    @metric = select_metric(params[:metric], 'ncloc')
    @highlighted_metric = Metric.by_key(params[:highlight]) || @metric

    # selected resources
    if params[:rids]
      selected_rids= params[:rids]
    elsif params[:resource]
      highlighted_resource=Project.by_key(params[:resource])
      selected_rids=(highlighted_resource ? [highlighted_resource.id] : [])
    else
      selected_rids=[]
    end
    selected_rids=selected_rids.map { |r| r.to_i }


    # options
    options={}
    if params[:characteristic_id]
      @characteristic=Characteristic.find(params[:characteristic_id])
    elsif params[:characteristic]
      @characteristic=Characteristic.find(:first, :conditions => ['characteristics.kee=? AND characteristics.enabled=?', params[:characteristic], true])
    end
    options[:characteristic]=@characteristic
    if params[:period] && Api::Utils.valid_period_index?(params[:period])
      @period=params[:period].to_i
      options[:period]=@period
    end

    # load data
    @drilldown = Drilldown.new(@resource, @metric, selected_rids, self, options)

    @highlighted_resource=@drilldown.highlighted_resource
    if @highlighted_resource.nil? && @drilldown.columns.empty?
      @highlighted_resource=@resource
    end

    @display_viewers = display_metric_viewers?(@highlighted_resource || @resource)
  end

  def issues
    @rule=Rule.by_key_or_id(params[:rule])
    @period=params[:period].to_i if params[:period].present? && params[:period].to_i>0
    @severity = params[:severity]
  end


  private

  def select_metric(metric_key, default_key)
    metric=nil
    if metric_key
      metric=Metric::by_key(metric_key)
    end
    if metric.nil?
      metric=Metric::by_key(default_key)
    end
    metric
  end

  def display_metric_viewers?(resource)
    return resource.file?
  end

end

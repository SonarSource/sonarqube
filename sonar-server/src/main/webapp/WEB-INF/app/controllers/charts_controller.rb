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

class ChartsController < ApplicationController

  DEFAULT_TRENDS_WIDTH = 700
  DEFAULT_TRENDS_HEIGHT = 250

  CHART_COLORS = ["4192D9", "800000", "A7B307", "913C9F", "329F4D"]

  def trends
    resource=Project.by_key(params[:id])
    bad_request("Unknown resource") unless resource
    access_denied unless has_role?(:user, resource)

    metric_keys=params[:metrics]
    metrics=[]
    if metric_keys
      metric_keys.split(',').each do |key|
        metric=Metric.by_key(key)
        metrics<<metric if metric
      end
    end

    bad_request("Unknown metrics") if metrics.empty?

    width=(params[:w] ? params[:w].to_i : DEFAULT_TRENDS_WIDTH)
    height=(params[:h] ? params[:h].to_i : DEFAULT_TRENDS_HEIGHT)
    display_legend = (params[:legend] ? params[:legend]=='true' : true)

    options={}
    if params[:from]
      options[:from]=Date::strptime(params[:from])
    end
    if params[:to]
      options[:to]=Date::strptime(params[:to])
    end

    stream = TrendsChart.png_chart(width, height, resource, metrics, params[:locale] || I18n.locale.to_s, display_legend, options)
    send_data stream, :type => 'image/png', :disposition => 'inline'
  end
end

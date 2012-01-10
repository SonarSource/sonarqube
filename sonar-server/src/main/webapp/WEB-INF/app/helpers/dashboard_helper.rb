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
module DashboardHelper
  include WidgetPropertiesHelper
  include MetricsHelper

  def formatted_value(measure, default='')
    measure ? measure.formatted_value : default
  end

  def measure(metric_key)
    @snapshot.measure(metric_key)
  end

  def period_select_options(snapshot, index)
    label=period_label(snapshot, index)
    if label
      selected=(params[:period]==index.to_s ? 'selected' : '')
      "<option value='#{index}' #{selected}>&Delta; #{label}</option>"
    else
      nil
    end
  end

  def violation_period_select_options(snapshot, index)
    return nil if snapshot.nil? || snapshot.project_snapshot.nil?
    mode=snapshot.project_snapshot.send "period#{index}_mode"
    mode_param=snapshot.project_snapshot.send "period#{index}_param"
    date=snapshot.project_snapshot.send "period#{index}_date"

    if mode
      if mode=='days'
        label = message('added_over_x_days', :params => mode_param.to_s)
      elsif mode=='version'
        label = message('added_since_version', :params => mode_param.to_s)
      elsif mode=='previous_analysis'
        if !date.nil?
          label = message('added_since_previous_analysis_detailed', :params => date.strftime("%Y %b. %d").to_s)
        else
          label = message('added_since_previous_analysis')
        end
      elsif mode=='date'
        label = message('added_since', :params => date.strftime("%Y %b %d").to_s)
      end
      if label
        selected=(params[:period]==index.to_s ? 'selected' : '')
        "<option value='#{index}' #{selected}>#{label}</option>"
      end
    else
      nil
    end

  end

  def measure_or_variation_value(measure)
    if measure
      @period_index ? measure.variation(@period_index) : measure.value
    else
      nil
    end
  end
end
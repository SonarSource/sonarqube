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
module DashboardHelper
  include WidgetPropertiesHelper

  def formatted_value(measure, default='')
    measure ? measure.formatted_value : default
  end

  def measure(metric_key)
    @snapshot.measure(metric_key)
  end

  def variation_select_option(snapshot, index)
    return nil if snapshot.nil? || snapshot.project_snapshot.nil?
    mode=snapshot.project_snapshot.send "var_mode_#{index}"
    mode_param=snapshot.project_snapshot.send "var_label_#{index}"

    if mode
      if mode=='days'
        label = "Last %s days" % mode_param
      elsif mode=='version'
        label = "Version %s" % mode_param
      end
      if label
        selected=(params[:var]==index.to_s ? 'selected' : '')
        "<option value='#{index}' #{selected}>#{label}</option>"
      end
    else
      nil
    end

  end
end
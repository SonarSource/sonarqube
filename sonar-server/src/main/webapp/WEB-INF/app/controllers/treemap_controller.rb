#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2013 SonarSource
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
class TreemapController < ApplicationController
  helper :metrics

  def index
    verify_ajax_request
    require_parameters :html_id, :resource

    if params[:size_metric].present?
      size_metric=Metric.by_key(params[:size_metric])
      bad_request('Unknown metric: ' + params[:size_metric]) unless size_metric
    end

    if params[:color_metric].present?
      color_metric=Metric.by_key(params[:color_metric])
      bad_request('Unknown metric: ' + params[:color_metric]) unless color_metric
    end

    resource = Project.by_key(params[:resource])
    bad_request('Unknown resource: ' + params[:resource]) unless resource
    bad_request('Data not available') unless resource.last_snapshot
    access_denied unless has_role?(:user, resource)
    resource = resource.permanent_resource

    filter = MeasureFilter.new
    filter.set_criteria_value(:baseId, resource.id)
    filter.set_criteria_value(:onBaseComponents, 'true')
    filter.set_criteria_value(:display, 'treemap')
    filter.set_criteria_value(:tmSize, size_metric.key) if size_metric
    filter.set_criteria_value(:tmColor, color_metric.key) if color_metric
    filter.execute(self, :user => current_user)

    render :text => filter.display.html
  end

end

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

  SECTION=Navigation::SECTION_RESOURCE

  def measures
    metric = params[:metric] || 'ncloc'
    period = params[:period].to_i if params[:period].present? && params[:period].to_i > 0
    if period
      return redirect_to(ApplicationController.root_context + "/component_measures/metric/#{metric}?id=#{url_encode(@resource.key)}&period=#{period}")
    else
      return redirect_to(ApplicationController.root_context + "/component_measures/metric/#{metric}?id=#{url_encode(@resource.key)}")
    end
  end

  def issues
    @rule=Rule.by_key_or_id(params[:rule])
    @period=params[:period].to_i if params[:period].present? && params[:period].to_i>0
    @severity = params[:severity]
  end

end

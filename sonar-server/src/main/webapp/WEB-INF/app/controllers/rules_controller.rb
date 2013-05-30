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
class RulesController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION

  # GET /rules/show/<key>
  # This URL is used by the Eclipse plugin
  #
  # ==== Optional parameters
  # 'layout' is false to remove sidebar and headers
  #
  # Example: GET /rules/show/squid:AvoidCycles
  #
  def show
    require_parameters :id

    @key=params[:id]
    if @key.to_i==0
      parts=@key.split(':')
      @rule=Rule.first(:conditions => ['plugin_name=? and plugin_rule_key=?', parts[0], parts[1]])
    else
      @rule=Rule.find(@key)
    end
    @page_title=@rule.name if @rule

    if params[:modal] == 'true'
      render :partial => 'show_modal'
    end
  end
end

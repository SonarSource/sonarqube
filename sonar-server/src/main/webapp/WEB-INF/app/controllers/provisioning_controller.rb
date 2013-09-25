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
class ProvisioningController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION

  before_filter :admin_required

  def index
    @tabs = provisionable_qualifiers

    @selected_tab = params[:qualifiers]
    @selected_tab = 'TRK' unless @tabs.include?(@selected_tab)

    params['pageSize'] = 20
    params['qualifiers'] = @selected_tab

    @query_results = Internal.component_api.findProvisioned(:qualifiers)
  end

  private

  def provisionable_qualifiers
    Java::OrgSonarServerUi::JRubyFacade.getInstance().getQualifiersWithProperty('hasRolePolicy')
  end

end

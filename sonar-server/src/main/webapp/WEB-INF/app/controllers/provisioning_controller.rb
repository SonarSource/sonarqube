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

  before_filter :admin_required
  verify :method => :delete, :only => [:delete], :redirect_to => {:action => :index}

  SECTION=Navigation::SECTION_CONFIGURATION

  def index
    @tabs = provisionable_qualifiers

    @selected_tab = params[:qualifiers]
    @selected_tab = 'TRK' unless @tabs.include?(@selected_tab)

    params['pageSize'] = 20
    params['qualifiers'] = @selected_tab

    @query_result = Internal.component_api.findProvisionedProjects(params)
  end

  def create
    verify_post_request
    access_denied unless is_admin?
    @key = params[:key]
    @name = params[:name]
    @qualifier = params[:qualifier]

    begin
      bad_request('provisioning.missing.key') if @key.blank?
      bad_request('provisioning.missing.name') if @name.blank?

      Internal.component_api.createComponent(@key, @name, 'PRJ', @qualifier)
      Internal.permissions.applyDefaultPermissionTemplate(@key)

      redirect_to :action => 'index'
    rescue Exception => e
      flash[:error]= Api::Utils.message(e.message)
      render :partial => 'create_form', :qualifier => @qualifier, :key => @key, :name => @name, :status => 400
    end
  end

  def create_form
    @key = params[:key]
    @name = params[:name]
    @qualifier = params[:qualifier]
    render :partial => 'create_form'
  end

  private

  def provisionable_qualifiers
    Java::OrgSonarServerUi::JRubyFacade.getInstance().getQualifiersWithProperty('hasRolePolicy')
  end

end

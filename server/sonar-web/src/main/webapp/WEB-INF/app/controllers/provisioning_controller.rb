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
class ProvisioningController < ApplicationController

  before_filter :admin_required
  verify :method => :delete, :only => [:delete], :redirect_to => {:action => :index}

  SECTION=Navigation::SECTION_CONFIGURATION

  def index
    access_denied unless has_role?("provisioning")
    params['qualifiers'] = 'TRK'

    @query_result = Api::Utils.insensitive_sort(
      Internal.component_api.findProvisionedProjects(params)
    ) { |p| p.key }
  end

  def create
    verify_post_request
    @id = params[:id]
    @key = params[:key]
    @name = params[:name]
    @branch = params[:branch]

    begin
      bad_request('provisioning.missing.key') if @key.blank?
      bad_request('provisioning.missing.name') if @name.blank?

      Internal.component_api.createComponent(@key, @branch, @name, nil)

      redirect_to :action => 'index'
    rescue Exception => e
      flash.now[:error]= Api::Utils.message(e.message)
      render :partial => 'create_form', :key => @key, :branch => @branch, :name => @name, :status => 400
    end
  end

  def create_form
    @id = params[:id]
    @key = params[:key]
    @name = params[:name]
    render :partial => 'create_form'
  end

  def delete_form
    @id = params[:id]
    render :partial => 'delete_form'
  end

  def delete
    access_denied unless has_role?("provisioning")

    @id = params[:id].to_i
    project = Project.first(:conditions => {:id => @id})
    Java::OrgSonarServerUi::JRubyFacade.getInstance().deleteResourceTree(project.key)
    flash.now[:notice]= Api::Utils.message('resource_viewer.resource_deleted')
    redirect_to :action => 'index'
  end

  private

end

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

# since 4.2
class Api::JavaWsController < Api::ApiController

  before_filter :check_authentication, :unless => ['skip_authentication_check_for_batch']

  # no need to check if WS can be accessed when DB is not up-to-date, this is dealt with in
  # Platform and ServerComponents classes
  skip_before_filter :check_database_version

  def index
    ws_request = Java::OrgSonarServerWs::ServletRequest.new(servlet_request, params.to_java)
    ws_response = Java::OrgSonarServerWs::ServletResponse.new()
    engine = Java::OrgSonarServerPlatform::Platform.component(Java::OrgSonarServerWs::WebServiceEngine.java_class)
    engine.execute(ws_request, ws_response, params[:wspath], params[:wsaction])

    ws_response.getHeaderNames().to_a.each do |name|
      response.header[name] = ws_response.getHeader(name)
    end

    # response is already written to HttpServletResponse
    render :text => ws_response.stream().output().toByteArray(),
           :status => ws_response.stream().httpStatus(),
           :content_type => ws_response.stream().mediaType()
  end

  def redirect_to_ws_listing
    redirect_to :action => 'index', :wspath => 'api/webservices', :wsaction => 'list'
  end


  def skip_authentication_check_for_batch
    (params[:wspath]=='batch' && params[:wsaction]=='index') ||
      (params[:wspath]=='batch' && params[:wsaction]=='file') ||
      (params[:wspath]=='api/system' && params[:wsaction]=='db_migration_status') ||
      (params[:wspath]=='api/system' && params[:wsaction]=='status')
  end

end

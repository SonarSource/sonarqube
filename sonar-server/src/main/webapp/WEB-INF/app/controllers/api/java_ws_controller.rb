#
# SonarQube, open source software quality management tool.
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

# since 4.2
class Api::JavaWsController < Api::ApiController
  def index
    respond_to do |format|
      format.json { execute('application/json') }
      format.xml { execute('application/xml') }
      format.csv { execute('text/csv') }
      format.text { execute('text/plain')}
    end
  end

  private
  def execute(media_type)
    ws_request = Java::OrgSonarServerWs::ServletRequest.new(servlet_request)

    # servlet_request is declared in jruby-rack but not servlet_response !
    # request.env must be used.
    ws_response = Java::OrgSonarServerWs::ServletResponse.new(request.env['java.servlet_response'])

    engine = Java::OrgSonarServerPlatform::Platform.component(Java::OrgSonarServerWs::WebServiceEngine.java_class)
    engine.execute(ws_request, ws_response, params[:wspath], params[:wsaction])

    # response is already written to HttpServletResponse
    render :nothing => true
  end
end

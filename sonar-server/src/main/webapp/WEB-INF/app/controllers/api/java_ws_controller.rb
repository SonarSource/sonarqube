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
    ws_request = Java::OrgSonarServerWs::Request.new(params)
    ws_request.setPost(request.post?)
    ws_request.setMediaType(media_type)
    engine = Java::OrgSonarServerPlatform::Platform.component(Java::OrgSonarServerWs::WebServiceEngine.java_class)
    ws_response = engine.execute(ws_request, params[:wspath], params[:wsaction])
    render :text => ws_response.writer(), :status => ws_response.status(), :content_type => media_type
  end
end

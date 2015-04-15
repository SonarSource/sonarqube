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

class ProfilesController < ApplicationController

  def index

  end

  def show
    render :action => 'index'
  end

  def changelog
    render :action => 'index'
  end

  def compare
    render :action => 'index'
  end

  # GET /profiles/export?name=<profile name>&language=<language>&format=<exporter key>
  def export
    language = params[:language]
    if params[:name].blank?
      profile = Internal.qprofile_service.getDefault(language)
    else
      profile = Internal.qprofile_loader.getByLangAndName(language, CGI::unescape(params[:name]))
    end
    not_found('Profile not found') unless profile

    if params[:format].blank?
      # standard sonar format
      result = Internal.qprofile_service.backup(profile.getKee())
      send_data(result, :type => 'text/xml', :disposition => 'inline')
    else
      exporter_key = params[:format]
      result = Internal.qprofile_exporters.export(profile.getKee(), exporter_key)
      send_data(result, :type => Internal.qprofile_exporters.mimeType(exporter_key), :disposition => 'inline')
    end
  end

end

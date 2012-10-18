#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#

require "json"

class Api::SynchroController < Api::ApiController

  # curl http://localhost:9000/api/synchro -v
  def index
    database_factory = java_facade.getCoreComponentByClassname('org.sonar.server.database.LocalDatabaseFactory')

    path = database_factory.createDatabaseForLocalMode()

    hash = {:path => path}

    respond_to do |format|
      format.json { render :json => jsonp(hash) }
    end
  end
end

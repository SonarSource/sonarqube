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

  # curl http://localhost:9000/api/synchro?resource=<resource> -v [-u user:password]
  def index
    require_parameters :resource
    load_resource()

    database_factory = java_facade.getCoreComponentByClassname('org.sonar.core.persistence.LocalDatabaseFactory')
    dbFileContent = database_factory.createDatabaseForLocalMode(@resource.id)

    send_data String.from_java_bytes(dbFileContent)
  end

  private

  def load_resource
    resource_id = params[:resource]
    @resource = Project.by_key(resource_id)
    return not_found("Resource [#{resource_id}] not found") if @resource.nil?
    return access_denied unless is_user?(@resource)
  end
end


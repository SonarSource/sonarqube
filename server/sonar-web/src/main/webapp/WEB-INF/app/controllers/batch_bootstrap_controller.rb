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

# Since 3.4
class BatchBootstrapController < Api::ApiController

  # SONAR-4211 Access to index should not require authentication
  skip_before_filter :check_authentication, :only => 'index'

  # GET /batch_bootstrap/db?project=<key or id>
  def db
    has_dryrun_role = has_role?('dryRunScan')
    return render_unauthorized("You're not authorized to execute a dry run analysis. Please contact your SonarQube administrator.") if !has_dryrun_role
    project = load_project()
    return render_unauthorized("You're not authorized to access to project '" + project.name + "', please contact your SonarQube administrator") if project && !has_role?(:user, project)
    db_content = java_facade.createDatabaseForPreview(project && project.id)

    send_data String.from_java_bytes(db_content)
  end


  private

  def render_unauthorized(message, status=403)
    respond_to do |format|
      format.json { render :text => message, :status => status }
      format.xml { render :text => message, :status => status }
      format.text { render :text => message, :status => status }
    end
  end

  def load_project
    if params[:project].present?
      Project.by_key(params[:project])
    else
      nil
    end
  end

  def to_json_property(property, project_key=nil)
    hash={:k => property.key, :v => property.text_value.to_s}
    hash[:p]=project_key if project_key
    hash
  end

  def allowed?(property_key, dryRun, has_scan_role)
    if property_key.end_with?('.secured')
      property_key.include?('.license') ? true : (!dryRun && has_scan_role)
    else
      true
    end
  end
end

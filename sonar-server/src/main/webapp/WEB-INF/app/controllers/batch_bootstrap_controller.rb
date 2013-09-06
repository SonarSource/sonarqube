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

# Since 3.4
class BatchBootstrapController < Api::ApiController

  # SONAR-4211 Access to index should not require authentication
  skip_before_filter :check_authentication, :only => 'index'

  # GET /batch_bootstrap/db?project=<key or id>
  def db
    has_dryrun_role = has_role?(Java::OrgSonarCorePermission::Permission::DRY_RUN_EXECUTION)
    return render_unauthorized("You're not authorized to execute a dry run analysis. Please contact your SonarQube administrator.") if !has_dryrun_role
    project = load_project()
    return render_unauthorized("You're not authorized to access to project '" + project.name + "', please contact your SonarQube administrator") if project && !has_role?(:user, project)
    db_content = java_facade.createDatabaseForDryRun(project && project.id)

    send_data String.from_java_bytes(db_content)
  end

  # PUT /batch_bootstrap/evict?project=<key or id>
  def evict
    has_scan_role = has_role?(Java::OrgSonarCorePermission::Permission::SCAN_EXECUTION)
    return render_unauthorized("You're not authorized to execute any SonarQube analysis. Please contact your SonarQube administrator.") if !has_scan_role

    project = load_project()
    return render_unauthorized("You're not authorized to access to project '" + project.name + "', please contact your SonarQube administrator") if project && !has_scan_role && !has_role?(:user, project)

    if project
      Property.set(Java::OrgSonarCoreDryrun::DryRunCache::SONAR_DRY_RUN_CACHE_LAST_UPDATE_KEY, java.lang.System.currentTimeMillis, project.root_project.id)
      render_success('dryRun DB evicted')
    else
      render_bad_request('missing projectId')
    end
  end

  # GET /batch_bootstrap/properties?[project=<key or id>][&dryRun=true|false]
  def properties
    dryRun = params[:dryRun].present? && params[:dryRun] == "true"
    has_dryrun_role = has_role?(Java::OrgSonarCorePermission::Permission::DRY_RUN_EXECUTION)
    has_scan_role = has_role?(Java::OrgSonarCorePermission::Permission::SCAN_EXECUTION)

    return render_unauthorized("You're not authorized to execute any SonarQube analysis. Please contact your SonarQube administrator.") if (!has_dryrun_role && !has_scan_role)
    return render_unauthorized("You're only authorized to execute a local (dry run) SonarQube analysis without pushing the results to the SonarQube server. Please contact your SonarQube administrator.") if (!dryRun && !has_scan_role)

    keys=Set.new
    properties=[]

    # project properties
    root_project = load_project()
    return render_unauthorized("You're not authorized to access to project '" + root_project.name + "', please contact your SonarQube administrator") if root_project && !has_scan_role && !has_role?(:user, root_project)

    if root_project
      # bottom-up projects
      projects=[root_project].concat(root_project.ancestor_projects)
      projects.each do |project|
        Property.find(:all, :conditions => ['resource_id=? and user_id is null', project.id]).each do |prop|
          properties<<prop if keys.add? prop.key
        end
      end
    end

    # global properties
    Property.find(:all, :conditions => 'resource_id is null and user_id is null').each do |prop|
      properties<<prop if keys.add? prop.key
    end

    # apply security
    properties = properties.select{|prop| allowed?(prop.key, dryRun, has_scan_role)}

    json_properties=properties.map { |property| to_json_property(property) }

    render :json => JSON(json_properties)
  end

  # GET /batch_bootstrap/index
  def index
    redirect_to ApplicationController.root_context.to_s + "/deploy/bootstrap/index.txt"
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

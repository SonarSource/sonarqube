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
    project = load_project()
    db_content = java_facade.createDatabaseForDryRun(project ? project.id : nil)

    send_data String.from_java_bytes(db_content)
  end

  # GET /batch_bootstrap/properties?[project=<key or id>]
  def properties
    keys=Set.new
    properties=[]
    
    # project properties
    root_project = load_project()
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
    has_user_role=has_role?(:user, root_project)
    has_admin_role=has_role?(:admin, root_project)
    properties = properties.select{|prop| allowed?(prop.key, has_user_role, has_admin_role)}
    
    json_properties=properties.map { |property| to_json_property(property) }

    render :json => JSON(json_properties)
  end

  # GET /batch_bootstrap/index
  def index
    redirect_to ApplicationController.root_context.to_s + "/deploy/bootstrap/index.txt"
  end

  private

  def load_project
    if params[:project].present?
      project = Project.by_key(params[:project])
      return access_denied if project && !has_role?(:user, project)
      project
    else
      nil
    end
  end

  def to_json_property(property, project_key=nil)
    hash={:k => property.key, :v => property.text_value.to_s}
    hash[:p]=project_key if project_key
    hash
  end

  def allowed?(property_key, has_user_role, has_admin_role)
    if property_key.end_with?('.secured')
      property_key.include?('.license') ? has_user_role : has_admin_role
    else
      true
    end
  end
end

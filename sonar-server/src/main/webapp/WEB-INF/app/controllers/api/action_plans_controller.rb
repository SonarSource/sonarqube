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

class Api::ActionPlansController < Api::ApiController

  #
  # GET /api/action_plans/search?project=<project>
  #
  # -- Example
  # curl -v -u 'http://localhost:9000/api/action_plans/search?project=org.sonar.Sample'
  #
  def search
    require_parameters :project

    action_plans = Internal.issues.findActionPlanStats(params[:project])
    hash = {:actionPlans => action_plans.map { |plan| ActionPlan.to_hash(plan)}}

    respond_to do |format|
      format.json { render :json => jsonp(hash) }
      format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'actionPlans') }
    end
  end

  #
  # POST /api/action_plans/create
  #
  # -- Mandatory parameters
  # 'name' is the name of the action plan
  # 'project' is the key of the project to link the action plan to
  #
  # -- Optional parameters
  # 'description' is the plain-text description
  # 'deadLine' is the due date of the action plan. Format is 'year-month-day', for instance, '2013-12-31'.
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/action_plans/create?name=Current&project=org.sonar.Sample'
  #
  def create
    verify_post_request
    require_parameters :project, :name

    result = Internal.issues.createActionPlan(params)
    render_result(result)
  end

  #
  # POST /api/action_plans/delete
  #
  # -- Mandatory parameters
  # 'key' is the action plan key
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/action_plans/delete?key=9b6f89c0-3347-46f6-a6d1-dd6c761240e0'
  #
  def delete
    verify_post_request
    require_parameters :key

    result = Internal.issues.deleteActionPlan(params[:key])
    render_result(result)
  end

  #
  # POST /api/action_plans/update
  #
  # -- Mandatory parameters
  # 'name' is the name of the action plan
  #
  # -- Optional parameters
  # 'description' is the plain-text description
  # 'deadLine' is the due date of the action plan. Format is 'year-month-day', for instance, '2013-12-31'.
  #
  # -- Information
  # 'project' cannot be update
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/action_plans/update?key=9b6f89c0-3347-46f6-a6d1-dd6c761240e0&name=Current'
  #
  def update
    verify_post_request
    require_parameters :key, :name

    result = Internal.issues.updateActionPlan(params[:key], params)
    render_result(result)
  end

  #
  # POST /api/action_plans/close
  #
  # -- Mandatory parameters
  # 'key' is the action plan key
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/action_plans/close?key=9b6f89c0-3347-46f6-a6d1-dd6c761240e0'
  #
  def close
    verify_post_request
    require_parameters :key

    result = Internal.issues.closeActionPlan(params[:key])
    render_result(result)
  end

  #
  # POST /api/action_plans/open
  #
  # -- Mandatory parameters
  # 'key' is the action plan key
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/action_plans/open?key=9b6f89c0-3347-46f6-a6d1-dd6c761240e0'
  #
  def open
    verify_post_request
    require_parameters :key

    result = Internal.issues.openActionPlan(params[:key])
    render_result(result)
  end


  private

  def render_result(result)
    http_status = (result.ok ? 200 : 400)
    hash = result_to_hash(result)
    hash[:actionPlan] = ActionPlan.to_hash(result.get) if result.get

    respond_to do |format|
      # if the request header "Accept" is "*/*", then the default format is the first one (json)
      format.json { render :json => jsonp(hash), :status => result.httpStatus }
      format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'sonar', :status => http_status) }
    end
  end

  def result_to_hash(result)
    hash = {}
    if result.errors and !result.errors.empty?
      hash[:errors] = result.errors().map do |error|
        {
            :msg => (error.text ? error.text : Api::Utils.message(error.l10nKey, :params => error.l10nParams))
        }
      end
    end
    hash
  end

end
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

  before_filter :admin_required, :only => [ :create, :delete, :update, :close, :open ]

  #
  # GET /api/action_plan/show?key=<key>
  #
  # -- Example
  # curl -v -u 'http://localhost:9000/api/action_plans/show?key=9b6f89c0-3347-46f6-a6d1-dd6c761240e0'
  #
  def show
    require_parameters :key

    action_plan = Internal.issues.findActionPlan(params[:key])
    hash = {}
    hash[:actionPlans] = action_plan_to_hash(action_plan) if action_plan

    respond_to do |format|
      format.json { render :json => jsonp(hash) }
      format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'actionPlans') }
    end
  end


  #
  # GET /api/action_plans/search?project=<project>
  #
  # -- Example
  # curl -v -u 'http://localhost:9000/api/action_plans/search?project=org.sonar.Sample'
  #
  def search
    require_parameters :project

    action_plans = Internal.issues.findActionPlanStats(params[:project])
    hash = {:actionPlans => action_plans.map { |plan| action_plan_to_hash(plan)}}

    respond_to do |format|
      format.json { render :json => jsonp(hash) }
      format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'actionPlans') }
    end
  end

  #
  # POST /api/action_plans/create
  #
  # -- Mandatory parameters
  # 'name' is the action plan name
  # 'project' is the project key to link the action plan to
  #
  # -- Optional parameters
  # 'description' is the plain-text description
  # 'deadLine' is the due date of the action plan. Format is 'day/month/year', for instance, '31/12/2013'.
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/action_plans/create?name=Current&project=org.sonar.Sample'
  #
  def create
    verify_post_request
    require_parameters :project, :name

    result = Internal.issues.createActionPlan(params)
    if result.ok()
      action_plan = result.get()
      render :json => jsonp({:actionPlan => action_plan_to_hash(action_plan)})
    else
      render_result_error(result)
    end
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
    if result.ok()
      render_success('Action plan deleted')
    else
      render_result_error(result)
    end
  end

  #
  # POST /api/action_plans/update
  #
  # -- Optional parameters
  # 'name' is the action plan name
  # 'project' is the project key to link the action plan to
  # 'description' is the plain-text description
  # 'deadLine' is the due date of the action plan. Format is 'day/month/year', for instance, '31/12/2013'.
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/action_plans/update?key=9b6f89c0-3347-46f6-a6d1-dd6c761240e0&name=Current'
  #
  def update
    verify_post_request
    require_parameters :key

    result = Internal.issues.updateActionPlan(params[:key], params)
    if result.ok()
      action_plan = result.get()
      render :json => jsonp({:actionPlan => action_plan_to_hash(action_plan)})
    else
      render_result_error(result)
    end
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
    if result.ok()
      action_plan = result.get()
      render :json => jsonp({:actionPlan => action_plan_to_hash(action_plan)})
    else
      render_result_error(result)
    end
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
    if result.ok()
      action_plan = result.get()
      render :json => jsonp({:actionPlan => action_plan_to_hash(action_plan)})
    else
      render_result_error(result)
    end
  end


  private

  def action_plan_to_hash(action_plan)
    hash = {:key => action_plan.key(), :name => action_plan.name(), :status => action_plan.status()}
    hash[:project] = action_plan.projectKey() if action_plan.projectKey() && !action_plan.projectKey().blank?
    hash[:desc] = action_plan.description() if action_plan.description() && !action_plan.description().blank?
    hash[:userLogin] = action_plan.userLogin() if action_plan.userLogin()
    hash[:deadLine] = Api::Utils.format_datetime(action_plan.deadLine()) if action_plan.deadLine()
    hash[:totalIssues] = action_plan.totalIssues() if action_plan.respond_to?('totalIssues')
    hash[:openIssues] = action_plan.openIssues() if action_plan.respond_to?('openIssues')
    hash[:createdAt] = Api::Utils.format_datetime(action_plan.createdAt()) if action_plan.createdAt()
    hash[:updatedAt] = Api::Utils.format_datetime(action_plan.updatedAt()) if action_plan.updatedAt()
    hash
  end

  def error_to_hash(msg)
    {:msg => message(msg.text(), {:params => msg.params()}).capitalize}
  end

  def render_result_error(result)
    hash = {:errors => result.errors().map { |error| error_to_hash(error) }}
    respond_to do |format|
      format.json { render :json => jsonp(hash), :status => 400}
      format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'errors', :status => 400)}
    end
  end

end
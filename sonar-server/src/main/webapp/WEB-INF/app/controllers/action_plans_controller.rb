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

class ActionPlansController < ApplicationController

  SECTION=Navigation::SECTION_RESOURCE
  before_filter :load_resource

  def index
    load_action_plans()
  end

  def edit
    @action_plan = find_by_key(params[:plan_key])
    load_action_plans()
    render 'index'
  end

  def save
    verify_post_request
    options = {'project' => @resource.key, 'name' => params[:name], 'description' => params[:description], 'deadLine' => params[:deadline]}

    exiting_action_plan = find_by_key(params[:plan_key]) unless params[:plan_key].blank?
    if exiting_action_plan
      action_plan_result = Internal.issues.updateActionPlan(params[:plan_key], options)
    else
      action_plan_result = Internal.issues.createActionPlan(options)
    end

    if action_plan_result.ok()
      @action_plan = action_plan_result.get()
      redirect_to :action => 'index', :id => @resource.id
    else
      flash[:error] = action_plan_result.errors().map{|error| error.text ? error.text : Api::Utils.message(error.l10nKey, :params => error.l10nParams)}.join('<br/>')
      load_action_plans()
      render 'index'
    end
  end

  def delete
    verify_post_request
    Internal.issues.deleteActionPlan(params[:plan_key])
    redirect_to :action => 'index', :id => @resource.id
  end

  def change_status
    verify_post_request
    action_plan = find_by_key(params[:plan_key])
    if action_plan
      if action_plan.status == 'OPEN'
        Internal.issues.closeActionPlan(params[:plan_key])
      else
        Internal.issues.openActionPlan(params[:plan_key])
      end
    end
    redirect_to :action => 'index', :id => @resource.id
  end

  private

  def load_resource
    @resource=Project.by_key(params[:id])
    return redirect_to home_path unless @resource
    access_denied unless has_role?(:admin, @resource)
  end

  def load_action_plans
    action_plans = Internal.issues.findActionPlanStats(@resource.key)
    @open_action_plans = action_plans.select {|plan| plan.isOpen()}
    @closed_action_plans = action_plans.reject {|plan| plan.isOpen()}
    users = Api.users.find('logins' => (@open_action_plans + @closed_action_plans).collect {|action_plan| action_plan.userLogin()}.join(","))
    @users = Hash[users.collect { |user| [user.login(), user.name()] }]
  end

  def find_by_key(key)
    Internal.issues.findActionPlan(key)
  end

end

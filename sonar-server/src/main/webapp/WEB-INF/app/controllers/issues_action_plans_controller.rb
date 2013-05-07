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

class IssuesActionPlansController < ApplicationController

  SECTION=Navigation::SECTION_RESOURCE
  before_filter :load_resource
  verify :method => :post, :only => [:save, :delete, :change_status], :redirect_to => {:action => :index}

  def index
    load_action_plans()
  end

  def edit
    @action_plan = find_by_key(params[:plan_key])
    load_action_plans()
    render 'index'
  end

  def save
    exiting_action_plan = find_by_key(params[:plan_key]) unless params[:plan_key].blank?
    options = {'projectId' => @resource.id, 'name' => params[:name], 'description' => params[:description]}

    unless params[:deadline].blank?
      begin
        deadline = DateTime.strptime(params[:deadline], '%d/%m/%Y')
        # we check if the date is today or in the future
        if deadline > 1.day.ago
          options['deadLine'] = Api::Utils.format_datetime(deadline)
        else
          date_not_valid = message('action_plans.date_cant_be_in_past')
        end
      rescue
        date_not_valid = message('action_plans.date_not_valid')
      end
    end

    if date_not_valid
      # TODO check errors
      flash[:error] = date_not_valid
      load_action_plans()
      render 'index'
    else
      if exiting_action_plan
        @action_plan = Internal.issues.updateActionPlan(@action_plan.key, options)
      else
        @action_plan = Internal.issues.createActionPlan(options)
      end
      redirect_to :action => 'index', :id => @resource.id
    end
  end

  def delete
    Internal.issues.deleteActionPlan(params[:plan_key])
    redirect_to :action => 'index', :id => @resource.id
  end

  def change_status
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
    @open_action_plans = Internal.issues.findOpenActionPlanStats(@resource.key)
    @closed_action_plans = Internal.issues.findClosedActionPlanStats(@resource.key)
  end

  def find_by_key(key)
    Internal.issues.findActionPlan(key)
  end

end

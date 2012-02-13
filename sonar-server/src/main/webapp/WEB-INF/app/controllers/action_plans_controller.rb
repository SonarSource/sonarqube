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

class ActionPlansController < ApplicationController

  SECTION=Navigation::SECTION_RESOURCE
  before_filter :load_resource
  verify :method => :post, :only => [:save, :delete, :change_status], :redirect_to => {:action => :index}

  def index
    load_action_plans()
  end

  def edit
    @action_plan = ActionPlan.find params[:plan_id]
    load_action_plans()
    render 'index'
  end

  def save
    @action_plan = ActionPlan.find params[:plan_id] unless params[:plan_id].blank?
    unless @action_plan
      @action_plan = ActionPlan.new(:user_login => current_user.login,
                                    :project_id => @resource.id,
                                    :status => ActionPlan::STATUS_OPEN)
    end
    
    @action_plan.name = params[:name]
    @action_plan.description = params[:description]
    unless params[:deadline].blank?
      begin
        deadline = DateTime.strptime(params[:deadline], '%d/%m/%Y')
        # we check if the date is today or in the future
        if deadline > 1.day.ago
          @action_plan.deadline = deadline
        else
          date_not_valid = message('action_plans.date_cant_be_in_past')
        end 
      rescue
        date_not_valid = message('action_plans.date_not_valid')
      end
    end

    if date_not_valid || !@action_plan.valid?
      @action_plan.errors.add :base, date_not_valid if date_not_valid
      flash[:error] = @action_plan.errors.full_messages.join('<br/>')
      load_action_plans()
      render 'index'
    else
      @action_plan.save
      redirect_to :action => 'index', :id => @resource.id
    end
  end

  def delete
    action_plan = ActionPlan.find params[:plan_id]
    action_plan.destroy
    redirect_to :action => 'index', :id => @resource.id
  end

  def change_status
    action_plan = ActionPlan.find params[:plan_id]
    if action_plan
      action_plan.status = action_plan.open? ? ActionPlan::STATUS_CLOSED : ActionPlan::STATUS_OPEN
      action_plan.save
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
    @open_action_plans = ActionPlan.find(:all, :conditions => ['status=? AND project_id=?', ActionPlan::STATUS_OPEN, @resource.id], :include => 'reviews', :order => 'deadline ASC')
    @closed_action_plans = ActionPlan.find(:all, :conditions => ['status=? AND project_id=?', ActionPlan::STATUS_CLOSED, @resource.id], :include => 'reviews', :order => 'deadline DESC')
  end

end

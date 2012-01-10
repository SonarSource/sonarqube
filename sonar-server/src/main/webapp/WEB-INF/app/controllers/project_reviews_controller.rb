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

class ProjectReviewsController < ApplicationController

  SECTION=Navigation::SECTION_RESOURCE

  verify :method => :post,
         :only => [:assign, :flag_as_false_positive, :save_comment, :delete_comment, :change_status,
                   :link_to_action_plan, :unlink_from_action_plan],
         :redirect_to => {:action => :error_not_post}
  helper SourceHelper, UsersHelper

  # lists all the reviews of a project, filtered using the same parameters as for the review WS API
  def index
    @project=Project.by_key(params[:projects])
    
    if @project
      access_denied unless has_role?(:user, @project)
      
      found_reviews = Review.search(params)
      @reviews = select_authorized(:user, found_reviews, :project)
      if found_reviews.size != @reviews.size
        @security_exclusions = true
      end
    else
      render :text => "<b>Listing reviews without a project reference is not possible</b>. Go to review search service instead."
    end
  end
  

  # Used for the permalink, e.g. http://localhost:9000/project_reviews/view/1
  def view
    @review = Review.find(params[:id], :include => ['project'])
    @resource = @review.project
    if has_role?(:user, @review.project)
      render 'project_reviews/_view', :locals => {:review => @review}
    else
      render :text => "<b>Cannot access this review</b> : access denied."
    end
  end


  #
  #
  # ACTIONS FROM REVIEW SERVICE PAGE
  #
  #

  def show
    @review = Review.find(params[:id], :include => ['project'])
    @resource = @review.project
    if has_role?(:user, @resource)
      render :partial => 'project_reviews/view'
    else
      render :text => "access denied"
    end
  end

  # GET
  def assign_form
    @review = Review.find(params[:id])
    render :partial => "assign_form"
  end

  # POST
  def assign
    @review = Review.find(params[:id], :include => ['project'])
    @resource = @review.project
    unless has_rights_to_modify?(@resource)
      render :text => "<b>Cannot edit the review</b> : access denied."
      return
    end

    assignee = nil
    if params[:me]=='true'
      assignee = current_user

    elsif params[:assignee_login].present?
      assignee = findUserByLogin(params[:assignee_login])
    end

    @review.reassign(current_user, assignee, params)
    render :partial => 'project_reviews/view'
  end

  # GET
  def comment_form
    @review = Review.find(params[:id])
    if !params[:comment_id].blank? && @review
      @comment = @review.comments.find(params[:comment_id])
    end
    render :partial => 'project_reviews/comment_form'
  end

  # POST
  def save_comment
    @review = Review.find(params[:id], :include => ['project'])
    @resource = @review.project
    unless has_rights_to_modify?(@resource)
      render :text => "<b>Cannot create the comment</b> : access denied."
      return
    end

    unless params[:text].blank?
      if params[:comment_id]
        @review.edit_comment(current_user, params[:comment_id].to_i, params[:text])
      else
        @review.create_comment(:user => current_user, :text => params[:text])
      end
    end

    render :partial => "project_reviews/view"
  end

  # GET
  def false_positive_form
    @review = Review.find(params[:id])
    render :partial => 'project_reviews/false_positive_form'
  end

  # POST
  def flag_as_false_positive
    @review = Review.find(params[:id], :include => ['project'])
    @resource = @review.project
    unless has_rights_to_modify?(@resource)
      render :text => "<b>Cannot create the comment</b> : access denied."
      return
    end

    @review.set_false_positive(params[:false_positive]=='true', current_user, params)
    render :partial => "project_reviews/view"
  end

  # POST
  def delete_comment
    @review = Review.find(params[:id], :include => ['project'])
    @resource = @review.project
    unless has_rights_to_modify?(@resource)
      render :text => "<b>Cannot delete the comment</b> : access denied."
      return
    end

    if @review
      @review.delete_comment(current_user, params[:comment_id].to_i)
    end
    render :partial => "project_reviews/view"
  end

  def change_status_form
    @review = Review.find(params[:id])
    render :partial => 'project_reviews/change_status_form'
  end

  # POST
  def change_status
    @review = Review.find(params[:id], :include => ['project'])
    @resource = @review.project
    unless has_rights_to_modify?(@resource)
      render :text => "<b>Cannot change the status</b> : access denied."
      return
    end

    if @review.resolved?
      @review.reopen(current_user, params)
    else
      # for the moment, if a review is not open, it can only be "RESOLVED"
      @review.resolve(current_user, params)
    end

    render :partial => "project_reviews/view"
  end

  # GET
  def change_severity_form
    render :partial => 'project_reviews/change_severity_form'
  end

  # POST
  def change_severity
    @review=Review.find(params[:id], :include => 'project')
    @resource = @review.project
    unless has_rights_to_modify?(@resource)
      render :text => "<b>Cannot change severity</b> : access denied."
      return
    end

    @review.set_severity(params[:severity], current_user, params)
    render :partial => "project_reviews/review"
  end

  # GET
  def action_plan_form
    @review = Review.find(params[:id])
    @action_plans = ActionPlan.open_by_project_id(@review.project_id)
    render :partial => 'project_reviews/action_plan_form'
  end
  
  # POST
  def link_to_action_plan
    @review = Review.find(params[:id])
    @resource = @review.project
    unless has_rights_to_modify?(@resource)
      render :text => "<b>Cannot link to action plan</b> : access denied."
      return
    end
    
    action_plan = params[:action_plan_id].to_i==-1 ? nil : ActionPlan.find(params[:action_plan_id])
    @review.link_to_action_plan(action_plan, current_user, params)

    render :partial => "project_reviews/review"
  end
  
  # POST
  def unlink_from_action_plan
    @review = Review.find(params[:id])
    @resource = @review.project
    unless has_rights_to_modify?(@resource)
      render :text => "<b>Cannot link to action plan</b> : access denied."
      return
    end
    
    @review.link_to_action_plan(nil, current_user, params)

    render :partial => "project_reviews/review"
  end
  
  
  #
  #
  # ACTIONS FROM THE REVIEW WIDGETS
  #
  #

  # GET
  def widget_reviews_list
    @snapshot = Snapshot.find(params[:snapshot_id])
    unless @snapshot && has_role?(:user, @snapshot)
      render :text => "<b>Cannot access the reviews of this project</b>: access denied."
      return
    end

    @dashboard_configuration=Api::DashboardConfiguration.new(nil, :period_index => params[:period], :snapshot => @snapshot)
    render :partial => 'project/widgets/reviews/reviews_list'
  end
  
  
  
  ## -------------- PRIVATE -------------- ##
  private

  def findUserByLogin(login)
    User.find(:first, :conditions => ["login = ?", login])
  end

  def has_rights_to_modify?(object)
    current_user && has_role?(:user, object)
  end

  def error_not_post
    render :text => "Create actions must use POST method."
  end

end

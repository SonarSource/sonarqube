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

class ReviewsController < ApplicationController

  SECTION=Navigation::SECTION_HOME

  verify :method => :post,
         :only => [:violation_assign, :violation_flag_as_false_positive, :violation_change_severity,
                   :violation_save_comment, :violation_delete_comment, :violation_change_status,
                   :violation_link_to_action_plan, :violation_unlink_from_action_plan],
         :redirect_to => {:action => :error_not_post}
  helper SourceHelper, UsersHelper

  def index
    init_params()
    search_reviews()
  end

  # Used for the "OLD" permalink "http://localhost:9000/reviews/view/1"
  # => Since Sonar 2.13, permalinks are "http://localhost:9000/project_reviews/view/1" and are displayed in the context of the project
  def view
    redirect_to :controller => 'project_reviews', :action => 'view', :id => params[:id]
  end


  #
  #
  # ACTIONS FROM VIOLATIONS TAB OF RESOURCE VIEWER
  #
  #

  # GET
  def display_violation
    violation = RuleFailure.find(params[:id])
    render :partial => "resource/violation", :locals => {:violation => violation}
  end

  # GET
  def violation_assign_form
    @violation = RuleFailure.find(params[:id], :include => 'review')
    render :partial => "violation_assign_form"
  end

  # POST
  def violation_assign
    violation = RuleFailure.find(params[:id], :include => 'snapshot')
    unless has_rights_to_modify?(violation.snapshot)
      render :text => "<b>Cannot edit the review</b> : access denied."
      return
    end
    sanitize_violation(violation)

    violation.build_review(:user_id => current_user.id)
    assignee=nil
    if params[:me]=='true'
      assignee = current_user

    elsif params[:assignee_login].present?
      assignee = findUserByLogin(params[:assignee_login])
    end
    violation.review.reassign(current_user, assignee, params)
    violation.save

    render :partial => "resource/violation", :locals => {:violation => violation}
  end

  # GET
  def violation_change_severity_form
    render :partial => 'reviews/violation_change_severity_form'
  end

  # POST
  def violation_change_severity
    violation=RuleFailure.find(params[:id], :include => 'snapshot')
    unless has_rights_to_modify?(violation.snapshot)
      render :text => "<b>Cannot change severity</b> : access denied."
      return
    end
    sanitize_violation(violation)

    if violation.review.nil?
      violation.build_review(:user_id => current_user.id)
    end
    violation.review.set_severity(params[:severity], current_user, params)
    # refresh the violation that has been modified
    violation.reload

    render :partial => "resource/violation", :locals => {:violation => violation}
  end

  # GET
  def violation_false_positive_form
    @violation = RuleFailure.find(params[:id])
    render :partial => 'reviews/violation_false_positive_form'
  end

  # POST
  def violation_flag_as_false_positive
    violation=RuleFailure.find(params[:id], :include => 'snapshot')
    unless has_rights_to_modify?(violation.snapshot)
      render :text => "<b>Cannot switch on the violation</b> : access denied."
      return
    end
    sanitize_violation(violation)

    if violation.review.nil?
      violation.build_review(:user_id => current_user.id)
    end
    violation.review.set_false_positive(params[:false_positive]=='true', current_user, params)

    # refresh the violation that has been modified when setting the review to false positive
    violation.reload

    render :partial => "resource/violation", :locals => {:violation => violation}
  end

  # GET
  def violation_comment_form
    @violation = RuleFailure.find params[:id]
    if !params[:comment_id].blank? && @violation.review
      @comment = @violation.review.comments.find(params[:comment_id])
    end
    render :partial => 'reviews/violation_comment_form'
  end

  # POST
  def violation_save_comment
    violation = RuleFailure.find(params[:id], :include => 'snapshot')
    unless has_rights_to_modify?(violation.snapshot)
      render :text => "<b>Cannot create the comment</b> : access denied."
      return
    end
    sanitize_violation(violation)

    unless violation.review
      assignee = findUserByLogin(params[:assignee_login]) unless params[:assignee_login].blank?
      violation.create_review!(
        :assignee => assignee,
        :user => current_user)
    end

    unless params[:text].blank?
      if params[:comment_id]
        violation.review.edit_comment(current_user, params[:comment_id].to_i, params[:text])
      else
        violation.review.create_comment(:user => current_user, :text => params[:text])
      end
    end

    render :partial => "resource/violation", :locals => {:violation => violation}
  end

  # POST
  def violation_delete_comment
    violation = RuleFailure.find(params[:id], :include => 'snapshot')
    unless has_rights_to_modify?(violation.snapshot)
      render :text => "<b>Cannot delete the comment</b> : access denied."
      return
    end
    sanitize_violation(violation)
    if violation.review
      violation.review.delete_comment(current_user, params[:comment_id].to_i)
    end
    render :partial => "resource/violation", :locals => {:violation => violation}
  end

  # GET
  def violation_change_status_form
    @violation = RuleFailure.find(params[:id], :include => 'review')
    render :partial => 'reviews/violation_change_status_form'
  end

  # POST
  def violation_change_status
    violation = RuleFailure.find(params[:id], :include => 'snapshot')
    unless has_rights_to_modify?(violation.snapshot)
      render :text => "<b>Cannot delete the comment</b> : access denied."
      return
    end
    sanitize_violation(violation)

    if violation.review.nil?
      violation.build_review(:user_id => current_user.id)
    end

    if violation.review.resolved?
      violation.review.reopen(current_user, params)
    else
      # for the moment, if a review is not open, it can only be "RESOLVED"
      violation.review.resolve(current_user, params)
    end

    render :partial => "resource/violation", :locals => {:violation => violation}
  end

  # GET
  def violation_action_plan_form
    @violation = RuleFailure.find(params[:id], :include => ['review', 'snapshot'])
    @action_plans = ActionPlan.open_by_project_id(@violation.snapshot.root_project_id)
    render :partial => 'reviews/violation_action_plan_form'
  end
  
  # POST
  def violation_link_to_action_plan
    violation = RuleFailure.find(params[:id], :include => 'snapshot')
    unless has_rights_to_modify?(violation.snapshot)
      render :text => "<b>Cannot link to action plan</b> : access denied."
      return
    end
    sanitize_violation(violation)
    
    if violation.review.nil?
      violation.build_review(:user_id => current_user.id)
    end
    action_plan = params[:action_plan_id].to_i==-1 ? nil : ActionPlan.find(params[:action_plan_id])
    violation.review.link_to_action_plan(action_plan, current_user, params)

    render :partial => "resource/violation", :locals => {:violation => violation}
  end
  
  # POST
  def violation_unlink_from_action_plan
    violation = RuleFailure.find(params[:id], :include => 'snapshot')
    unless has_rights_to_modify?(violation.snapshot)
      render :text => "<b>Cannot link to action plan</b> : access denied."
      return
    end
    violation.review.link_to_action_plan(nil, current_user, params)

    render :partial => "resource/violation", :locals => {:violation => violation}
  end


  ## -------------- PRIVATE -------------- ##
  private

  def findUserByLogin(login)
    User.find(:first, :conditions => ["login = ?", login])
  end

  def init_params
    default_user = (current_user ? current_user.login : '')
    @assignee_login = params[:assignee_login] || default_user
    @author_login = params[:author_login] || ''
    @severities = filter_any(params[:severities]) || ['']
    @statuses = filter_any(params[:statuses]) || [Review::STATUS_OPEN, Review::STATUS_REOPENED]
    @projects = filter_any(params[:projects]) || ['']
    @false_positives = params[:false_positives] || 'without'
    @id = params[:review_id] || ''
    @sort = params[:sort]
    @asc = params[:asc] == "true"
    @from = Time.parse(params[:from]) if params[:from]
    @to = Time.parse(params[:to]) if params[:to]
  end

  def filter_any(array)
    if array && array.size>1 && array.include?("")
      array=[""]
    end
    array
  end

  def search_reviews
    options = {}
    unless @statuses == ['']
      options['statuses']=@statuses.join(',')
    end
    unless @projects == ['']
      options['projects']=@projects.join(',')
    end
    unless @severities == ['']
      options['severities']=@severities.join(',')
    end
    if @author_login
      options['authors']=@author_login
    end
    if @assignee_login
      options['assignees']=@assignee_login unless @assignee_login.blank?
    end
    if @false_positives
      options['false_positives']=@false_positives
    end
    if @from
      options['from']=@from
    end
    if @to
      options['to']=@to
    end
    unless @id == ''
      if Api::Utils.is_integer? @id
        options['id'] = @id
      else
        options['id'] = -1
      end
    end
    options['sort'] = @sort unless @sort.blank?
    options['asc'] = @asc

    found_reviews = Review.search(options)
    @reviews = select_authorized(:user, found_reviews, :project)
    if found_reviews.size != @reviews.size
      @security_exclusions = true
    end
  end

  def has_rights_to_modify?(object)
    current_user && has_role?(:user, object)
  end

  def error_not_post
    render :text => "Create actions must use POST method."
  end

  def sanitize_violation(violation)
    # the field RULE_FAILURES.PERMANENT_ID is not set when upgrading to version 2.8.
    # It must be manually set when using a violation created before v2.8.
    if violation.permanent_id.nil?
      violation.permanent_id=violation.id
      violation.save
    end
  end
end

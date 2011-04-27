#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2011 SonarSource
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
         :only => [:assign, :comment_form, :flag_as_false_positive, 
                   :violation_assign, :violation_flag_as_false_positive,:violation_save_comment, :violation_delete_comment], 
         :redirect_to => {:action => :error_not_post}
  helper ReviewsHelper, MarkdownHelper, SourceHelper

  def index
    init_params()
    search_reviews()
  end
  
  def show
    @review=Review.find(params[:id], :include => ['resource', 'project'])
    render :partial => 'reviews/show'
  end

  # GET
  def assign_form
    @user_options = options_for_users()
    render :partial => "assign_form"
  end

  # POST
  def assign
    @review = Review.find (params[:id])
    unless current_user
      render :text => "<b>Cannot edit the review</b> : access denied."
      return
    end

    @review.assignee = User.find params[:assignee_id]
    @review.save

    render :partial => 'reviews/show'
  end

  # GET
  def comment_form
    @review = Review.find (params[:id])
    render :partial => 'reviews/comment_form'
  end

  # POST
  def save_comment
    @review = Review.find (params[:id])
    unless current_user
      render :text => "<b>Cannot create the comment</b> : access denied."
      return
    end

    if params[:comment_id]
      comment = @review.comments.find(params[:comment_id].to_i)
      if comment
        comment.text=params[:text]
        comment.save!
      end
    else
      @review.comments.create!(:user => current_user, :text => params[:text])
    end

    render :partial => "reviews/show"
  end

  # GET
  def false_positive_form
    render :partial => 'reviews/false_positive_form'
  end

  # POST
  def flag_as_false_positive
    @review = Review.find (params[:id])
    unless current_user
      render :text => "<b>Cannot create the comment</b> : access denied."
      return
    end
    
    RuleFailure.find( :all, :conditions => [ "permanent_id = ?", @review.rule_failure_permanent_id ] ).each do |violation|
      violation.switched_off=true
      violation.save!
    end

    @review.review_type = Review::TYPE_FALSE_POSITIVE
    @review.status = Review::STATUS_CLOSED
    @review.save!
    unless params[:comment].blank?
      @review.comments.create(:review_text => params[:comment], :user_id => current_user.id)
    end

    render :partial => "reviews/show"
  end


  #
  #
  # ACTIONS FROM VIOLATIONS TAB OF RESOURCE VIEWER
  #
  #

  # GET
  def display_violation
    violation = RuleFailure.find(params[:id])
    render :partial => "resource/violation", :locals => { :violation => violation }
  end

  # GET
  def violation_assign_form
    @user_options = options_for_users()
    render :partial => "violation_assign_form"
  end

  # POST
  def violation_assign
    violation = RuleFailure.find(params[:id])
    unless current_user
      render :text => "<b>Cannot edit the review</b> : access denied."
      return
    end
    sanitize_violation(violation)

    violation.build_review(:user_id => current_user.id)
    violation.review.assignee = User.find params[:assignee_id]
    violation.review.save!
    violation.save

    render :partial => "resource/violation", :locals => { :violation => violation }
  end

  # GET
  def violation_false_positive_form
    render :partial => 'reviews/violation_false_positive_form'
  end

  # POST
  def violation_flag_as_false_positive
    violation=RuleFailure.find params[:id]
    unless has_rights_to_modify?(violation)
      render :text => "<b>Cannot switch on the violation</b> : access denied."
      return
    end
    sanitize_violation(violation)
    false_positive=(params[:false_positive]=='true')
    violation.switched_off=false_positive
    violation.save!

    unless params[:comment].blank?
      if violation.review.nil?
        violation.build_review(:user_id => current_user.id)
      end
      violation.review.review_type=(false_positive ? Review::TYPE_FALSE_POSITIVE : Review::TYPE_VIOLATION)
      violation.review.status=(false_positive ? Review::STATUS_CLOSED : Review::STATUS_OPEN)
      violation.review.save!
      violation.review.comments.create(:review_text => params[:comment], :user_id => current_user.id)
    end

    render :partial => "resource/violation", :locals => { :violation => violation }
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
    violation = RuleFailure.find params[:id]
    unless has_rights_to_modify?(violation)
      render :text => "<b>Cannot create the comment</b> : access denied."
      return
    end
    sanitize_violation(violation)

    unless violation.review
      violation.create_review!(
          :assignee => (params['assign_to_me']=='me' ? current_user : nil),
          :user => current_user)
    end

    if params[:comment_id]
      comment=violation.review.comments.find(params[:comment_id].to_i)
      if comment
        comment.text=params[:text]
        comment.save!
      end
    else
      violation.review.comments.create!(:user => current_user, :text => params[:text])
    end

    render :partial => "resource/violation", :locals => { :violation => violation }
  end

  # POST
  def violation_delete_comment
    violation = RuleFailure.find params[:id]
    unless has_rights_to_modify?(violation)
      render :text => "<b>Cannot delete the comment</b> : access denied."
      return
    end
    sanitize_violation(violation)
    if violation.review
      comment=violation.review.comments.find(params[:comment_id].to_i)
      comment.delete if comment
    end
    render :partial => "resource/violation", :locals => { :violation => violation }
  end



  ## -------------- PRIVATE -------------- ##
  private

  def init_params
    @user_names = [["Any", ""]] + options_for_users
    default_user = (current_user ? [current_user.id.to_s] : [''])
    @authors = filter_any(params[:authors]) || ['']
    @assignees = filter_any(params[:assignees]) || default_user
    @severities = filter_any(params[:severities]) || ['']
    @statuses = filter_any(params[:statuses]) || [Review::STATUS_OPEN]
    @projects = filter_any(params[:projects]) || ['']
  end

  def options_for_users
    options=[]
    User.find( :all ).each do |user|
      username = user.name
      if current_user && current_user.id == user.id
        username = "Me (" + user.name + ")"
      end
      options<<[username, user.id.to_s]
    end
    options
  end

  def filter_any(array)
    if array && array.size>1 && array.include?("")
      array=[""]
    end
    array
  end

  def search_reviews
    conditions=['review_type<>:not_type']
    values={:not_type => Review::TYPE_FALSE_POSITIVE}

    unless @statuses == [""]
      conditions << "status in (:statuses)"
      values[:statuses]=@statuses
    end
    unless @severities == [""]
      conditions << "severity in (:severities)"
      values[:severities]=@severities
    end
    unless @authors == [""]
      conditions << "user_id in (:authors)"
      values[:authors]=@authors.map{|s| s.to_i}
    end
    unless @assignees == [""]
      conditions << "assignee_id in (:assignees)"
      values[:assignees]=@assignees.map{|s| s.to_i}
    end

    @reviews = Review.find( :all, :order => "created_at DESC", :conditions => [ conditions.join(" AND "), values] ).uniq
  end

  def has_rights_to_modify?(violation)
    current_user && has_role?(:user, violation.snapshot)
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

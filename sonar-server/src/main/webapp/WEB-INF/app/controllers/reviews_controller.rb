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
  helper SourceHelper, UsersHelper

  def index
    init_params()
    search_reviews()
  end
  
  # Used for the permalink, e.g. http://localhost:9000/reviews/view/1
  def view
    @review = Review.find(params[:id], :include => ['project'])
    if has_role?(:user, @review.project)
      render 'reviews/_view', :locals => {:review => @review}
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
    if has_role?(:user, @review.project)
      render :partial => 'reviews/view'
    else
      render :text => "access denied"
    end
  end

  # GET
  def assign_form
    render :partial => "assign_form"
  end

  # POST
  def assign
    @review = Review.find(params[:id], :include => ['project'])
    unless has_rights_to_modify?(@review.project)
      render :text => "<b>Cannot edit the review</b> : access denied."
      return
    end

    assignee = User.find params[:assignee_id] unless params[:assignee_id].blank?
    @review.assignee = assignee
    @review.save

    render :partial => 'reviews/view'
  end

  # GET
  def comment_form
    @review = Review.find(params[:id])
    if !params[:comment_id].blank? && @review
      @comment = @review.comments.find(params[:comment_id])
    end
    render :partial => 'reviews/comment_form'
  end

  # POST
  def save_comment
    @review = Review.find(params[:id], :include => ['project'])
    unless has_rights_to_modify?(@review.project)
      render :text => "<b>Cannot create the comment</b> : access denied."
      return
    end

    if params[:comment_id]
      @review.edit_comment(params[:comment_id].to_i, params[:text])
    else
      @review.create_comment(:user => current_user, :text => params[:text])
    end
    
    render :partial => "reviews/view"
  end

  # GET
  def false_positive_form
    render :partial => 'reviews/false_positive_form'
  end

  # POST
  def flag_as_false_positive
    @review = Review.find(params[:id], :include => ['project'])
    unless has_rights_to_modify?(@review.project)
      render :text => "<b>Cannot create the comment</b> : access denied."
      return
    end
    
    false_positive=(params[:false_positive]=='true')
    RuleFailure.find( :all, :conditions => [ "permanent_id = ?", @review.rule_failure_permanent_id ] ).each do |violation|
      violation.switched_off=false_positive
      violation.save!
    end

    @review.false_positive = false_positive
    @review.assignee = nil
    @review.save!
    unless params[:comment].blank?
      @review.comments.create(:review_text => params[:comment], :user_id => current_user.id)
    end

    render :partial => "reviews/view"
  end

  # POST
  def delete_comment
    @review = Review.find(params[:id], :include => ['project'])
    unless has_rights_to_modify?(@review.project)
      render :text => "<b>Cannot delete the comment</b> : access denied."
      return
    end
    
    if @review
      @review.delete_comment(params[:comment_id].to_i)
    end
    render :partial => "reviews/view"
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
    assignee = User.find params[:assignee_id] unless params[:assignee_id].blank?
    violation.review.assignee = assignee
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
    violation=RuleFailure.find(params[:id], :include => 'snapshot')
    unless has_rights_to_modify?(violation.snapshot)
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
      violation.review.false_positive=false_positive
      violation.review.assignee=nil
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
    violation = RuleFailure.find(params[:id], :include => 'snapshot')
    unless has_rights_to_modify?(violation.snapshot)
      render :text => "<b>Cannot create the comment</b> : access denied."
      return
    end
    sanitize_violation(violation)

    unless violation.review
      assignee = User.find params[:assignee_id] unless params[:assignee_id].blank?
      violation.create_review!(
          :assignee => assignee,
          :user => current_user)
    end

    if params[:comment_id]
      violation.review.edit_comment(params[:comment_id].to_i, params[:text])
    else
      violation.review.create_comment(:user => current_user, :text => params[:text])
    end

    render :partial => "resource/violation", :locals => { :violation => violation }
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
      violation.review.delete_comment(params[:comment_id].to_i)
    end
    render :partial => "resource/violation", :locals => { :violation => violation }
  end



  ## -------------- PRIVATE -------------- ##
  private

  def init_params
    default_user = (current_user ? current_user.id : '')
    @assignee_id = params[:assignee_id] || default_user
    @author_id = params[:author_id] || ''
    @severities = filter_any(params[:severities]) || ['']
    @statuses = filter_any(params[:statuses]) || [Review::STATUS_OPEN]
    @projects = filter_any(params[:projects]) || ['']
    @false_positives = params[:false_positives] || 'without'
    @id = params[:review_id] || ''
    @sort = params[:sort]
    @asc = params[:asc] == "true"
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
    if @author_id
      options['authors']=@author_id.to_s
    end
    if @assignee_id
      options['assignees']=@assignee_id.to_s
    end
    if @false_positives
      options['false_positives']=@false_positives
    end
    unless @id  == ''
      if is_number? @id
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

  def is_number?(s)
    true if Float(s) rescue false
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

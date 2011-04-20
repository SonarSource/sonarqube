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

  verify :method => :post, :only => [  :create, :create_comment ], :redirect_to => { :action => :error_not_post }

  def index
    init_params

    @reviews = []
    unless params.blank?
      find_reviews_for_user_query
    end
  end

  def list
    reviews = find_reviews_for_rule_failure params[:rule_failure_permanent_id]
    render :partial => "list", :locals => { :reviews => reviews }
  end

  def display_violation
    violation = find_last_rule_failure_with_permanent_id params[:rule_failure_permanent_id]
    render :partial => "resource/violation", :locals => { :violation => violation }
  end

  def form
    rule_failure = find_last_rule_failure_with_permanent_id params[:rule_failure_permanent_id]
    @review = Review.new
    @review.rule_failure_permanent_id = rule_failure.permanent_id
    @review_comment = ReviewComment.new
    @review_comment.review_text = ""
    if params[:switch_off]
      @review.review_type = "f-positive"
    else
      @review.review_type = Review.default_type
    end
    render :partial => "form"
  end

  def create
    rule_failure = find_last_rule_failure_with_permanent_id params[:review][:rule_failure_permanent_id]
    unless has_rights_to_create? rule_failure
      render :text => "<b>Cannot create the review</b> : access denied."
      return
    end

    @review = Review.new(params[:review])
    @review.user = current_user
    if params[:assign_to_me]
      @review.assignee = current_user
    end
    @review.title = rule_failure.message
    @review.status = Review.default_status
    @review.severity = Sonar::RulePriority.to_s rule_failure.failure_level
    @review.resource = RuleFailure.find( @review.rule_failure_permanent_id, :include => ['snapshot'] ).snapshot.project
    @review_comment = ReviewComment.new(params[:review_comment])
    @review_comment.user = current_user
    @review.review_comments << @review_comment
    if @review.valid?
      if @review.review_type == "f-positive" 
        if rule_failure.get_open_review
          current_open_review = rule_failure.get_open_review
          current_open_review.status = "closed"
          current_open_review.save
        end
        rule_failure.switched_off = true
        rule_failure.save
      end
      @review.save
      @violation = rule_failure
    end
    render "create_result"
  end

  def form_comment
    @review_comment = ReviewComment.new
    @review_comment.user = current_user
    @review_comment.review_id = params[:review_id]
    @review_comment.review_text = ""
    @rule_failure_permanent_id = params[:rule_failure_permanent_id]
    if params[:update_comment]
      @update_comment = true
      @review_comment.review_text = params[:review_text]
    end
    render :partial => "form_comment"
  end

  def create_comment
    rule_failure = find_last_rule_failure_with_permanent_id params[:rule_failure_permanent_id]
    unless has_rights_to_create? rule_failure
      render :text => "<b>Cannot create the comment</b> : access denied."
      return
    end

    @review_comment = ReviewComment.new(params[:review_comment])
    @review_comment.user = current_user
    @rule_failure_permanent_id = params[:rule_failure_permanent_id]
    if @review_comment.valid?
      @review_comment.save
      # -- TODO : should create a Review#create_comment and put the following logic in it
      review = @review_comment.review
      review.updated_at = @review_comment.created_at
      review.save
      # -- End of TODO code --
      @violation = rule_failure
    end
    render "create_comment_result"
  end

  def update_comment
    review = Review.find params[:review_comment][:review_id]
    @review_comment = review.review_comments.last
    unless current_user && current_user.id == @review_comment.user_id
      render :text => "<b>Cannot modify the comment</b> : access denied."
      return
    end

    @review_comment.review_text = params[:review_comment][:review_text]
    @review_comment.created_at = DateTime.now
    @rule_failure_permanent_id = params[:rule_failure_permanent_id]
    if @review_comment.valid?
      @review_comment.save
      review.updated_at = @review_comment.updated_at
      review.save
      @violation = find_last_rule_failure_with_permanent_id review.rule_failure_permanent_id
    end
    render "create_comment_result"
  end
  
  def form_assign
    @user_options = add_all_users []
    @review_id = params[:review_id]
    @rule_failure_permanent_id = params[:rule_failure_permanent_id]
    render :partial => "form_assign"
  end
  
  def assign
    review = Review.find params[:review_id]
    unless current_user
      render :text => "<b>Cannot edit the review</b> : access denied."
      return
    end
    
    review.assignee = User.find params[:assignee_id]
    review.save
    violation = find_last_rule_failure_with_permanent_id review.rule_failure_permanent_id
    render :partial => "resource/violation", :locals => { :violation => violation }
  end
  
  def close_review
    review = Review.find params[:review_id]
    unless current_user
      render :text => "<b>Cannot edit the review</b> : access denied."
      return
    end
    
    review.status = "closed"
    review.save
    violation = find_last_rule_failure_with_permanent_id review.rule_failure_permanent_id
    render :partial => "resource/violation", :locals => { :violation => violation }
  end

  ## -------------- PRIVATE -------------- ##
  private

  def init_params
    @user_names = [["Any", ""]]
    default_user = [""]
    if current_user
      default_user = [current_user.id]
    end
    add_all_users @user_names
    @review_authors = filter_any(params[:review_authors]) || default_user
    @comment_authors = filter_any(params[:comment_authors]) || default_user
    @severities = filter_any(params[:severities]) || [""]
    @statuses = filter_any(params[:statuses]) || ["open"]
  end
  
  def add_all_users ( user_options )
    User.find( :all ).each do |user|
      userName = user.name
      if current_user && current_user.id == user.id
        userName = "Me (" + user.name + ")"
      end
      user_options << [userName, user.id.to_s]
    end
    return user_options
  end

  def filter_any(array)
    if array && array.size>1 && array.include?("")
      array=[""]
    end
    array
  end

  def find_reviews_for_user_query
    conditions=[]
    values={}

    unless @statuses == [""]
      conditions << "reviews.status in (:statuses)"
      values[:statuses]=@statuses
    end
    unless @severities == [""]
      conditions << "reviews.severity in (:severities)"
      values[:severities]=@severities
    end
    unless @review_authors == [""]
      conditions << "reviews.user_id in (:review_authors)"
      values[:review_authors]=@review_authors
    end
    unless @comment_authors == [""]
      conditions << "review_comments.user_id in (:comment_authors)"
      values[:comment_authors]=@comment_authors
    end

    @reviews = Review.find( :all, :order => "created_at DESC", :joins => :review_comments, :conditions => [ conditions.join(" AND "), values] ).uniq
  end

  def find_reviews_for_rule_failure ( rule_failure_permanent_id )
    return Review.find :all, :conditions => ['rule_failure_permanent_id=?', rule_failure_permanent_id]
  end

  def find_last_rule_failure_with_permanent_id ( rule_failure_permanent_id )
    return RuleFailure.last( :all, :conditions => [ "permanent_id = ?", rule_failure_permanent_id ], :include => ['snapshot'] )
  end

  def has_rights_to_create? ( rule_failure )
    return false unless current_user
    
    project = rule_failure.snapshot.root_project
    unless has_role?(:user, project)
      return false
    end
    return true
  end

  def error_not_post
    render :text => "Create actions must use POST method."
  end

end

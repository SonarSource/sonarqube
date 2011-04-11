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
      findReviewsForUserQuery
    end
  end
  
  def list
    reviews = findReviewsForRuleFailure params[:rule_failure_id]
    render :partial => "list", :locals => { :reviews => reviews }
  end
  
  def form
    @review = Review.new
    @review.rule_failure_id = params[:violation_id]
    @review.user = current_user
    @review.severity = Review.default_severity
    @review_comment = ReviewComment.new
    @review_comment.review_text = ""
    render :partial => "form"
  end
  
  def form_comment
    @review_comment = ReviewComment.new
    @review_comment.user = current_user
    @review_comment.review_id = params[:review_id]
    @review_comment.review_text = ""
    @rule_failure_id = params[:rule_failure_id]
    render :partial => "form_comment"
  end
  
  def create
    unless hasRightsToCreate? params[:review][:rule_failure_id]
      render :text => "<b>Cannot create the review</b> : access denied."
      return
    end
        
    @review = Review.new(params[:review])
    @review.user = current_user
    @review.status = Review.default_status
    @review.review_type = Review.default_type
    @review_comment = ReviewComment.new(params[:review_comment])
    @review_comment.user = current_user
    @review.review_comments << @review_comment
    if @review.valid?
      @review.save
      @reviews = findReviewsForRuleFailure @review.rule_failure_id
    end
    render "create_result"
  end
  
  def create_comment
    unless hasRightsToCreate? params[:rule_failure_id]
      render :text => "<b>Cannot create the comment</b> : access denied."
      return
    end
  
      @review_comment = ReviewComment.new(params[:review_comment])
      @review_comment.user = current_user
      @rule_failure_id = params[:rule_failure_id]
      if @review_comment.valid?
      @review_comment.save
      @reviews = findReviewsForRuleFailure @rule_failure_id
    end
      render "create_comment_result"
  end
  
  
  ## -------------- PRIVATE -------------- ##
  private
  
  def init_params
    @user_names = [["Any", ""]]
    default_user = [""]
    if current_user 
      @user_names << ["Me", current_user.id]
      default_user = [current_user.id]
    end
    User.find( :all ).each do |user|
      @user_names << [user.name, user.id.to_s]
    end
    @review_authors = filter_any(params[:review_authors]) || default_user
    @comment_authors = filter_any(params[:comment_authors]) || default_user
    @severities = filter_any(params[:severities]) || [""]
    @statuses = filter_any(params[:statuses]) || ["open"]
  end
  
  def filter_any(array)
    if array && array.size>1 && array.include?("")
      array=[""]
    end
    array
  end
  
  def findReviewsForUserQuery
    @conditions=""
    @values=[]
    @need_and = false;
    @need_or = false;
    add_sql_query_param "status", @statuses
    add_sql_query_param "severity", @severities
    add_sql_query_param "reviews.user_id", @review_authors
    add_sql_query_param "review_comments.user_id", @comment_authors
    
    @reviews = Review.find( :all, :order => "created_at DESC", :joins => :review_comments, :conditions => [@conditions] + @values ).uniq
  end
  
  def add_sql_query_param ( field, search_params )
    unless search_params == [""]
      @conditions += " AND" if @need_and
      @conditions += "("
      search_params.each do |search_param|
        @conditions += " OR" if @need_or
        @conditions += " " + field + "=?"
        @values << search_param
        @need_or = true
      end
      @conditions += ")"
      @need_or = false;
      @need_and = true;
    end
  end
  
  def findReviewsForRuleFailure ( rule_failure_id )
    return Review.find :all, :conditions => ['rule_failure_id=?', rule_failure_id]
  end
  
  def hasRightsToCreate? ( rule_failure_id )
    return false unless current_user
    
    project = RuleFailure.find( rule_failure_id, :include => ['snapshot'] ).snapshot.root_project
    unless has_role?(:user, project)
      return false
    end
    return true
  end
  
  def error_not_post
    render :text => "Create actions must use POST method."
  end
  
end

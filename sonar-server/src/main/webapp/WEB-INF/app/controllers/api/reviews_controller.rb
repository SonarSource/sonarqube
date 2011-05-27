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

require 'json'

class Api::ReviewsController < Api::ApiController

  # GETs should be safe (see http://www.w3.org/2001/tag/doc/whenToUseGet.html)
  verify :method => :put, :only => [ :update ]
  verify :method => :post, :only => [ :create ]
  
  def index
    convert_markdown=(params[:output]=='HTML')
    reviews=select_authorized(:user, Review.search(params), :project)
    
    render_reviews(reviews, convert_markdown)
  end

  #
  # --- Creation of a review --- 
  #
  # POST /api/reviews
  # Required parameters:
  # - 'text' : the text of the comment
  # - 'violation_id' : the violation on which the review shoul be created
  #
  # Optional parameters :
  # - 'assignee' : login used to create a review directly assigned 
  # - 'false_positive' : if "true", creates a false-positive review
  #
  # Example :
  # - POST "/api/reviews/create?violation_id=1&assignee=fabrice&text=Hello%20World!
  # - POST "/api/reviews/create?violation_id=2&false_positive=true&text=No%20violation%20here
  #
  def create
    begin
      # 1- Get some parameters and check some of them
      convert_markdown=(params[:output]=='HTML')
      false_positive = params[:false_positive]=='true'
      assignee = find_user(params[:assignee])
      text = params[:text]
      raise "No 'text' parameter has been provided." unless text && !text.blank?
        
      # 2- Create the review
      if params[:violation_id]
        violation = RuleFailure.find(params[:violation_id], :include => ['snapshot', 'review'])
        unless has_rights_to_modify?(violation.snapshot)
          access_denied
          return
        end
        raise "Violation #"+violation.id.to_s+" already has a review." if violation.review
        sanitize_violation(violation)
        violation.create_review!(:assignee => assignee, :user => current_user)
      else
        raise "No 'violation_id' parameter has been provided."
      end
      
      # 3- Create the comment and handle false-positive
      review = violation.review
      if false_positive
        review.set_false_positive(false_positive, :user => current_user, :text => text, :violation_id => params[:violation_id])
      else
        review.create_comment(:user => current_user, :text => text)
      end
      
      # 4- And finally send back the review
      render_reviews([review], convert_markdown)
    
    rescue ApiException => e
      render_error(e.msg, e.code)

    rescue Exception => e
      render_error(e.message, 400)
    end
  end
  
  
  #
  # --- Update a review = reassign, flag as false positive, add/edit a comment --- 
  #
  # PUT /api/reviews
  # Required parameters:
  # - 'id' : the review id
  #
  # Optional parameters :
  # - 'text' : used to edit the text of the last comment (if the last comment belongs to the current user)
  # - 'new_text' : used to add a comment 
  # - 'assignee' : login used to create a review directly assigned. Use 'none' to unassign.
  # - 'false_positive' (true/false) : in conjunction with 'new_text' (mandatory in this case), changes the 
  #                                   state 'false_positive' of the review
  #
  # Example :
  # - PUT "/api/reviews/update?id=1&false_positive=true&new_text=Because
  # - PUT "/api/reviews/update?id=1&assignee=fabrice
  # - PUT "/api/reviews/update?id=1&assignee=none
  # - PUT "/api/reviews/update?id=1&new_text=New%20Comment!
  # - PUT "/api/reviews/update?id=1&text=Modified%20Comment!
  #
  def update
    begin
      # 1- Get some parameters
      convert_markdown=(params[:output]=='HTML')
      text = params[:text]
      new_text = params[:new_text]
      assignee = params[:assignee]
      false_positive = params[:false_positive]
        
      # 2- Get the review or create one
      raise "No 'id' parameter has been provided." unless params[:id]
      review = Review.find(params[:id], :include => ['project'])
      unless has_rights_to_modify?(review.project)
        access_denied
        return
      end
      
      # 3- Modify the review
      if !false_positive.blank?
        raise "'false_positive' parameter must be either 'true' or 'false'." unless false_positive=='true' || false_positive=='false'
        raise "'new_text' parameter is mandatory with 'false_positive'." if new_text.blank?
        review.set_false_positive(false_positive=='true', :user => current_user, :text => new_text)
      elsif !assignee.blank?
        if (assignee=='none')
          user = nil
        else
          user = find_user(assignee)
          raise "No user found with the following login: "+assignee unless user
        end
        review.reassign(user)
      elsif !new_text.blank?
        review.create_comment(:user => current_user, :text => new_text)
      elsif !text.blank?
        raise "You don't have the rights to edit the last comment of this review." unless review.comments.last.user == current_user
        review.edit_last_comment(text)
      else
        render_error("The given parameter combination is invalid for updating the review.", 403)
        return
      end
      
      # 4- And finally send back the review
      render_reviews([review], convert_markdown)
    
    rescue ApiException => e
      render_error(e.msg, e.code)

    rescue Exception => e
      render_error(e.message, 400)
    end
  end
  
  
  private
  
  def render_reviews(reviews, convert_markdown)
    respond_to do |format|
      format.json { render :json => jsonp(Review.reviews_to_json(reviews, convert_markdown)) }
      format.xml {render :xml => Review.reviews_to_xml(reviews, convert_markdown)}
      format.text { render :text => text_not_supported }
    end
  end
  
  def find_user(login)
    unless login.blank?
      users = User.find(:all, :conditions => ["login = ?", login])
      users[0] if users.size > 0
    end 
  end
  
  def has_rights_to_modify?(object)
    current_user && has_role?(:user, object)
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
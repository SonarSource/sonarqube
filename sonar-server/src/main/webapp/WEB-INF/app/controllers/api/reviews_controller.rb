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
  verify :method => :put, :only => [ :add_comment, :reassign, :resolve, :reopen ]
  verify :method => :post, :only => [ :create ]
  #verify :method => :delete, :only => [ :destroy ]

  #
  # --- Search reviews ---
  # Since 2.8
  #
  # GET /api/reviews
  # Optional parameters :
  # - 'statuses'
  # - 'resolutions' (since 2.9)
  # - 'severities'
  # - 'projects'
  # - 'resources'
  # - 'authors'
  # - 'assignees'
  #
  def index
    reviews=select_authorized(:user, Review.search(params), :project)

    render_reviews(reviews, params[:output] == 'HTML')
  end

  #
  # --- Creation of a review ---
  # Since 2.9
  #
  # POST /api/reviews
  # Required parameters:
  # - 'violation_id' : the violation on which the review shoul be created
  # - 'status' : the initial status (can be 'OPEN' or 'RESOLVED')
  # - 'comment' : the text of the comment
  #
  # Optional parameters :
  # - 'resolution' : if status 'RESOLVED', then resolution must be provided (can be 'FIXED' or 'FALSE-POSITIVE')
  # - 'assignee' : login used to create a review directly assigned
  #
  # Example :
  # - POST "/api/reviews/?violation_id=1&status=OPEN&assignee=admin&comment=Please%20fix%20this"
  # - POST "/api/reviews/?violation_id=2&status=RESOLVED&resolution=FALSE-POSITIVE&comment=No%20violation%20here"
  # - POST "/api/reviews/?violation_id=3&status=RESOLVED&resolution=FIXED&assignee=admin&comment=This%20violation%20was%20fixed%20by%20me"
  #
  def create
    begin
      # 1- Get some parameters and check some of them
      convert_markdown=(params[:output]=='HTML')
      assignee = find_user(params[:assignee])
      status = params[:status]
      resolution = params[:resolution]
      comment = params[:comment] || request.raw_post
      violation_id = params[:violation_id]
      raise "No 'violation_id' parameter has been provided." unless violation_id && !violation_id.blank?
      raise "No 'status' parameter has been provided." unless status && !status.blank?
      raise "No 'comment' parameter has been provided." unless comment && !comment.blank?

      Review.transaction do
        # 2- Create the review
        violation = RuleFailure.find(violation_id, :include => ['snapshot', 'review'])
        unless has_rights_to_modify?(violation.snapshot)
          access_denied
          return
        end
        raise "Violation #" + violation.id.to_s + " already has a review." if violation.review
        sanitize_violation(violation)
        violation.create_review!(:assignee => assignee, :user => current_user)
        review = violation.review

        # 3- Set status
        if status == Review::STATUS_OPEN
          review.create_comment(:user => current_user, :text => comment)
        elsif status == Review::STATUS_RESOLVED
          if resolution == 'FALSE-POSITIVE'
            review.set_false_positive(true, :user => current_user, :text => comment, :violation_id => violation_id)
          elsif resolution == 'FIXED'
            review.create_comment(:user => current_user, :text => comment)
            review.resolve(current_user)
          else
            raise "Incorrect resolution."
          end
        else
          raise "Incorrect status."
        end

        # 4- And finally send back the review
        render_reviews([review], convert_markdown)
      end
    rescue ApiException => e
      render_error(e.msg, e.code)

    rescue Exception => e
      render_error(e.message, 400)
    end
  end

  #
  # --- Add comment ---
  # Since 2.9
  #
  # PUT /api/reviews/add_comment
  # Required parameters:
  # - 'id' : the review id
  # - 'comment' : the text of the comment
  #
  # Example :
  # - PUT "/api/reviews/add_comment/1?comment=New%20Comment!"
  #
  def add_comment
    begin
      review = get_review(params[:id])
      review.transaction do
        comment = params[:comment] || request.raw_post
        if review.isClosed?
          raise "Closed review can not be commented."
        end
        raise "Comment must be provided." unless comment && !comment.blank?
        review.create_comment(:user => current_user, :text => comment)
      end
      render_reviews([review], params[:output] == 'HTML')
    rescue ApiException => e
      render_error(e.msg, e.code)
    rescue Exception => e
      render_error(e.message, 400)
    end
  end

  #
  # --- Reassign ---
  # Since 2.9
  #
  # PUT /api/reviews/reassign
  # Required parameters:
  # - 'id' : the review id
  # - 'assignee' : new assignee
  #
  # Example :
  # - PUT "/api/reviews/reassign/1?assignee=fabrice"
  # - PUT "/api/reviews/reassign/1?assignee="
  #
  def reassign
    begin
      review = get_review(params[:id])
      review.transaction do
        assignee = params[:assignee]
        if !review.isOpen? && !review.isReopened?
          raise "Only open review can be reassigned."
        end
        if assignee.blank?
          user = nil
        else
          user = find_user(assignee)
          raise "Assignee not found." unless user
        end
        review.reassign(current_user, user)
      end
      render_reviews([review], params[:output] == 'HTML')
    rescue ApiException => e
      render_error(e.msg, e.code)
    rescue Exception => e
      render_error(e.message, 400)
    end
  end

  #
  # --- Resolve ---
  # Since 2.9
  #
  # PUT /api/reviews/resolve
  # Required parameters:
  # - 'id' : the review id
  # - 'resolution' : can be 'FIXED' or 'FALSE-POSITIVE'
  # - 'comment' : the text of the comment
  #
  # Example :
  # - PUT "/api/reviews/resolve/1?resolution=FALSE-POSITIVE&comment=No%20violation%20here"
  # - PUT "/api/reviews/resolve/1?resolution=FIXED"
  # - PUT "/api/reviews/resolve/1?resolution=FIXED&comment=This%20violation%20was%20fixed%20by%20me"
  #
  def resolve
    begin
      review = get_review(params[:id])
      review.transaction do
        resolution = params[:resolution]
        comment = params[:comment] || request.raw_post
        if !review.isOpen? && !review.isReopened?
          raise "Only open review can be resolved."
        end
        if resolution == 'FALSE-POSITIVE'
          raise "Comment must be provided." unless comment && !comment.blank?
          review.set_false_positive(true, :user => current_user, :text => comment)
        elsif resolution == 'FIXED'
          review.create_comment(:user => current_user, :text => comment) unless comment.blank?
          review.resolve(current_user)
        else
          raise "Incorrect resolution."
        end
      end
      render_reviews([review], params[:output] == 'HTML')
    rescue ApiException => e
      render_error(e.msg, e.code)
    rescue Exception => e
      render_error(e.message, 400)
    end
  end

  #
  # --- Reopen ---
  # Since 2.9
  #
  # PUT /api/reviews/reopen
  # Required parameters:
  # - 'id' : the review id
  # - 'comment' : the text of the comment
  #
  # Example :
  # - PUT "/api/reviews/reopen/1"
  # - PUT "/api/reviews/reopen/1?comment=Not%20fixed"
  #
  def reopen
    begin
      review = get_review(params[:id])
      review.transaction do
        comment = params[:comment] || request.raw_post
        if !review.isResolved?
          raise "Only resolved review can be reopened."
        end
        if review.resolution == 'FALSE-POSITIVE'
          raise "Comment must be provided." unless comment && !comment.blank?
          review.set_false_positive(false, :user => current_user, :text => comment)
        else
          review.reopen(current_user)
          review.create_comment(:user => current_user, :text => comment) unless comment.blank?
        end
      end
      render_reviews([review], params[:output] == 'HTML')
    rescue ApiException => e
      render_error(e.msg, e.code)
    rescue Exception => e
      render_error(e.message, 400)
    end
  end


  private

  def get_review(id)
    raise "No 'id' parameter has been provided." unless id
    review = Review.find(id, :include => ['project'])
    raise ApiException.new(401, 'Unauthorized') unless has_rights_to_modify?(review.project)
    return review
  end

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

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

require 'json'

class Api::ReviewsController < Api::ApiController

  verify :method => :put, :only => [:add_comment, :reassign, :resolve, :reopen]
  verify :method => :post, :only => [:create]

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

  # Review of an existing violation or create a new violation.
  #
  # Since 2.9
  # POST /api/reviews
  #
  # ==== Requirements
  #
  # * If the violation must be created on a given line of a file, then source code must be available. It
  # means that it's not compatible with the property sonar.importSources=false.
  #
  # * Requires the USER role on the related project
  #
  # ==== Parameters
  #
  # To review an existing violation :
  # * 'violation_id' : the violation on which the review should be created
  #
  # To create a violation :
  # * 'rule_name' : the name of the rule in the repository "manual". If it does not exist then the rule is created.
  # * 'resource' : id or key of the resource to review
  # * 'line' : optional line. It starts from 1. If 0 then no specific line. Default value is 0.
  # * 'severity' : BLOCKER, CRITICAL, MAJOR, MINOR or INFO. Default value is MAJOR.
  # * 'cost' : optional numeric cost
  #
  # Other parameters :
  # * 'status' : the initial status (can be 'OPEN' or 'RESOLVED')
  # * 'comment' : the text of the comment
  # * 'resolution' (optional) : if status 'RESOLVED', then resolution must be provided (can be 'FIXED' or 'FALSE-POSITIVE')
  # * 'assignee' (optional) : login used to create a review directly assigned
  #
  # ==== Examples
  #
  # * Create a manual violation : POST /api/reviews?resource=MyFile&line=18&status=OPEN&rule_name=Performance%20Issue
  # * Review an existing violation : POST /api/reviews?violation_id=1&status=OPEN&assignee=admin&comment=Please%20fix%20this
  # * Flag an existing violation as false-positive : POST /api/reviews/?violation_id=2&status=RESOLVED&resolution=FALSE-POSITIVE&comment=No%20violation%20here
  # * Resolve an existing violation : POST /api/reviews/?violation_id=3&status=RESOLVED&resolution=FIXED&assignee=admin&comment=This%20violation%20was%20fixed%20by%20me
  #
  def create
    # Validate parameters
    convert_markdown=(params[:output]=='HTML')
    assignee = find_user(params[:assignee])
    status = params[:status]
    resolution = params[:resolution]
    comment = params[:comment] || request.raw_post
    bad_request("Missing parameter 'status'") if status.blank?
    bad_request("Missing parameter 'comment'") if comment.blank?
    review = nil

    Review.transaction do
      if params[:violation_id].present?
        # Review an existing violation
        violation = RuleFailure.find(params[:violation_id], :include => :rule)
        access_denied unless has_rights_to_modify?(violation.resource)
        bad_request("Violation is already reviewed") if violation.review
        sanitize_violation(violation)
        violation.create_review!(:assignee => assignee, :user => current_user, :manual_violation => false)

      else
        # Manually create a violation and review it
        bad_request("Missing parameter 'rule_name'") if params[:rule_name].blank?
        bad_request("Missing parameter 'resource'") if params[:resource].blank?
        resource = Project.by_key(params[:resource])
        access_denied unless resource && has_rights_to_modify?(resource)
        bad_request("Resource does not exist") unless resource.last_snapshot

        rule = Rule.find_or_create_manual_rule(params[:rule_name], has_role?(:admin))
        access_denied unless rule
        violation = rule.create_violation!(resource, params)
        violation.create_review!(:assignee => assignee, :user => current_user, :manual_violation => true)
      end

      # Set review status
      review = violation.review
      if status == Review::STATUS_OPEN
        review.create_comment(:user => current_user, :text => comment)
      elsif status == Review::STATUS_RESOLVED
        if resolution == Review::RESOLUTION_FALSE_POSITIVE
          review.set_false_positive(true, current_user, :text => comment)
        elsif resolution == Review::RESOLUTION_FIXED
          review.create_comment(:user => current_user, :text => comment)
          review.resolve(current_user)
        else
          bad_request("Incorrect resolution")
        end
      else
        bad_request("Incorrect status")
      end
    end

    # 5- And finally send back the review
    render_reviews([review], convert_markdown)
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
        if review.closed?
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
        if !review.open? && !review.reopened?
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
        if !review.open? && !review.reopened?
          raise "Only open review can be resolved."
        end
        if resolution == Review::RESOLUTION_FALSE_POSITIVE
          raise "Comment must be provided." unless comment && !comment.blank?
          review.set_false_positive(true, current_user, :text => comment)
        elsif resolution == Review::RESOLUTION_FIXED
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
        if !review.resolved?
          raise "Only resolved review can be reopened."
        end
        if review.resolution == Review::RESOLUTION_FALSE_POSITIVE
          raise "Comment must be provided." unless comment && !comment.blank?
          review.set_false_positive(false, current_user, :text => comment)
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
    review
  end

  def render_reviews(reviews, convert_markdown)
    respond_to do |format|
      format.json { render :json => jsonp(Review.reviews_to_json(reviews, convert_markdown)) }
      format.xml { render :xml => Review.reviews_to_xml(reviews, convert_markdown) }
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

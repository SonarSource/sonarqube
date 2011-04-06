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

	SECTION=Navigation::SECTION_CONFIGURATION
	
	def index
	end
	
	def form
	  @review = Review.new
	  @review.rule_failure_id = params[:violation_id]
	  @review.user = current_user
	  @review_data = ReviewData.new
	  @review_data.user = current_user
	  @review_data.review = @review
	  @review_data.review_text = "Enter your review here"
	  render "_form", :layout => false
	end
	
	def create
	  review = Review.new(params[:review])
	  review.user = current_user
	  review.save
      review_data = ReviewData.new(params[:review_data])
	  review_data.user = current_user
	  review_data.review_id = review.id
	  review_data.save
	  #render "_view", :layout => false
	end
	
	def cancel_create
	  render :nothing => true
	end
	
end

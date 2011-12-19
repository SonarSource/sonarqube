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

class ProjectReviewsController < ApplicationController

  SECTION=Navigation::SECTION_RESOURCE

  def index
    @project=Project.by_key(params[:projects])
    not_found("Project not found") unless @project
    access_denied unless is_admin?(@project)
    
    found_reviews = Review.search(params)
    @reviews = select_authorized(:user, found_reviews, :project)
    if found_reviews.size != @reviews.size
      @security_exclusions = true
    end

#    # table pagination
#    @page_size = 20
#    @page_size = params[:page_size].to_i if Api::Utils.is_number?(params[:page_size]) && params[:page_size].to_i > 5
#    @total_number = @reviews.size
#    if @reviews.size > @page_size
#      @page_id = (params[:page_id] ? params[:page_id].to_i : 1)
#      @page_count = @reviews.size / @page_size
#      @page_count += 1 if (@reviews.size % @page_size > 0)
#      from = (@page_id-1) * @page_size
#      to = (@page_id*@page_size)-1
#      to = @reviews.size-1 if to >= @reviews.size
#      @reviews = @reviews[from..to]
#    end
  end

end

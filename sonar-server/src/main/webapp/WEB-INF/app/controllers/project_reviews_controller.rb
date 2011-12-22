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
  end

end

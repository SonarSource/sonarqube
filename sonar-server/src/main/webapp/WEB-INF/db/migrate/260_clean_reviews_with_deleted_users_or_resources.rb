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

#
# Sonar 2.14
#
class CleanReviewsWithDeletedUsersOrResources < ActiveRecord::Migration

  class Review < ActiveRecord::Base
  end

  class ReviewComment < ActiveRecord::Base
  end

  def self.up
    Review.reset_column_information
    Review.find(:all, :include => ['resource', 'assignee', 'user']).each do |review|
      if review.resource_id && !review.resource
        # For http://jira.codehaus.org/browse/SONAR-3223
        review.destroy
      else
        # For http://jira.codehaus.org/browse/SONAR-3102
        must_save=false
        if review.user_id && !review.user
          review.user_id=nil
          must_save=true
        end
        if review.assignee_id && !review.assignee
          review.assignee_id=nil
          must_save=true
        end
        review.save if must_save
      end      
    end

    # For http://jira.codehaus.org/browse/SONAR-3102
    ReviewComment.reset_column_information
    ReviewComment.find(:all, :include => 'user').each do |comment|
      comment.delete if comment.user_id && !comment.user
    end   
    
  end

end

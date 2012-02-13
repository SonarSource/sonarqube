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
class FixReviewsWithDeletedUser < ActiveRecord::Migration

  def self.up
    # For http://jira.codehaus.org/browse/SONAR-3102
    Review.find(:all, :include => ['assignee', 'user']).each do |review|
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

    ReviewComment.find(:all, :include => 'user').each do |comment|
      comment.delete if comment.user_id && !comment.user
    end
  end

end

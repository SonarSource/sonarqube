#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2013 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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

    # http://jira.codehaus.org/browse/SONAR-3223
    orphans = Review.find_by_sql "SELECT r.id FROM reviews r WHERE r.resource_id is not null and not exists (select * from projects p WHERE p.id=r.resource_id)"
    orphans.each do |review|
      review.delete
    end



    # http://jira.codehaus.org/browse/SONAR-3102
    reviews_with_user_orphan = Review.find_by_sql "SELECT r.id FROM reviews r WHERE r.user_id is not null and not exists (select * from users u WHERE u.id=r.user_id)"
    reviews_with_user_orphan.each do |review|
      Review.update_all 'user_id=null', ['id=?', review.id]
    end

    reviews_with_assignee_orphan = Review.find_by_sql "SELECT r.id FROM reviews r WHERE r.assignee_id is not null and not exists (select * from users u WHERE u.id=r.assignee_id)"
    reviews_with_assignee_orphan.each do |review|
      Review.update_all 'assignee_id=null', ['id=?', review.id]
    end



    # http://jira.codehaus.org/browse/SONAR-3102
    ReviewComment.reset_column_information
    comments = ReviewComment.find_by_sql("SELECT c.id FROM review_comments c WHERE c.user_id is not null and not exists (select * from users u WHERE u.id=c.user_id)")
    comments.each do |comment|
      comment.delete
    end
  end

end

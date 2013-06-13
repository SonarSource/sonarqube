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
# Sonar 3.6
# See SONAR-4305
#
class DeleteReviewDuplications < ActiveRecord::Migration

  class Review < ActiveRecord::Base
  end

  def self.up
    duplicated_ids = ActiveRecord::Base.connection.select_rows('select rule_failure_permanent_id from reviews group by rule_failure_permanent_id having count(*) > 1')
    say_with_time "Remove #{duplicated_ids.size} duplicated reviews" do
      duplicated_ids.each do |id|
        reviews = Review.find(:all, :conditions => {:rule_failure_permanent_id => id})
        # delete all reviews except the last one
        reviews[0...-1].each do |review|
          Review.delete(review.id)
        end
      end
    end
  end
end
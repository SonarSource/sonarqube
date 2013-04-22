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
# Sonar 2.9
#
class ChangeFalsePositiveOnReviews < ActiveRecord::Migration

  class Review < ActiveRecord::Base
  end

  def self.up
    add_column 'reviews', 'resolution', :string, :limit => 200, :null => true

    Review.reset_column_information
    Review.update_all("status='RESOLVED', resolution='FALSE-POSITIVE'", "review_type='FALSE_POSITIVE'")
    
    remove_column 'reviews', 'review_type'
  end

end

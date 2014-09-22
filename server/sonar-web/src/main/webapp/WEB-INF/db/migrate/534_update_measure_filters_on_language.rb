#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
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
# SonarQube 4.4
# SONAR-5221
#
class UpdateMeasureFiltersOnLanguage < ActiveRecord::Migration

  class MeasureFilter < ActiveRecord::Base
  end

  def self.up
    MeasureFilter.reset_column_information
    MeasureFilter.all(:conditions => "data LIKE '%language%'").each do |filter|
      # Remove sort on language
      filter.data = filter.data.sub('sort=language', '')

      filter.data.scan(/cols=((.+?)(\||\z))?/) do |find|
        cols_data = find[0]
        # Remove display of language column
        filter.data = filter.data.sub(cols_data, cols_data.gsub(/language/, ''))
      end
      filter.save!
    end
  end
  
end

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
class RenameDbcleanerProperties < ActiveRecord::Migration

  class Property < ActiveRecord::Base
  end


  def self.up
    Property.reset_column_information
    rename('sonar.dbcleaner.monthsBeforeKeepingOnlyOneSnapshotByWeek', 'sonar.dbcleaner.weeksBeforeKeepingOnlyOneSnapshotByWeek')
    rename('sonar.dbcleaner.monthsBeforeKeepingOnlyOneSnapshotByMonth', 'sonar.dbcleaner.weeksBeforeKeepingOnlyOneSnapshotByMonth')
    rename('sonar.dbcleaner.monthsBeforeDeletingAllSnapshots', 'sonar.dbcleaner.weeksBeforeDeletingAllSnapshots')
  end

  private
  def self.rename(month_key, week_key)
    Property.find(:all, :conditions => ['prop_key=? and text_value is not null', month_key]).each do |month_property|
      Property.create(:prop_key => week_key, :resource_id => month_property.resource_id, :text_value => (month_property.text_value.to_i * 4).to_s)
    end
  end
end

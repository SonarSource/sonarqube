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
# SonarQube 5.1
#
class RenameSnapshotsLongDates < ActiveRecord::Migration
  def self.up
    remove_column 'snapshots', 'created_at'
    remove_column 'snapshots', 'build_date'
    remove_column 'snapshots', 'period1_date'
    remove_column 'snapshots', 'period2_date'
    remove_column 'snapshots', 'period3_date'
    remove_column 'snapshots', 'period4_date'
    remove_column 'snapshots', 'period5_date'
    rename_column 'snapshots', 'created_at_ms', 'created_at'
    rename_column 'snapshots', 'build_date_ms', 'build_date'
    rename_column 'snapshots', 'period1_date_ms', 'period1_date'
    rename_column 'snapshots', 'period2_date_ms', 'period2_date'
    rename_column 'snapshots', 'period3_date_ms', 'period3_date'
    rename_column 'snapshots', 'period4_date_ms', 'period4_date'
    rename_column 'snapshots', 'period5_date_ms', 'period5_date'
  end
end


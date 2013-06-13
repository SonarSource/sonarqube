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
# Sonar 2.5
#
class DeleteUnvalidProjectSnapshots < ActiveRecord::Migration

  class Snapshot < ActiveRecord::Base
  end

  def self.up
    Snapshot.reset_column_information
    snapshots=select_snapshots_without_measures
    delete_snapshots(snapshots)
  end

  def self.select_snapshots_without_measures
   snapshots=nil
   say_with_time "Select project snapshots without measures..." do
     snapshots=Snapshot.find_by_sql ["SELECT s.id FROM snapshots s WHERE s.scope='PRJ' and s.qualifier IN ('TRK', 'BRC') AND status='P' AND islast=? AND NOT EXISTS (select m.id from project_measures m WHERE m.snapshot_id=s.id)", false]
   end
   snapshots
  end

  def self.delete_snapshots(snapshots)
   if snapshots.size>0
     say_with_time "Delete #{snapshots.size} orphan snapshots..." do
       sids=snapshots.map{|s| s.id}
       page_size=100
       page_count=(sids.size/page_size)
       page_count+=1 if (sids.size % page_size)>0
       page_count.times do |page_index|
         page_sids=sids[page_index*page_size...(page_index+1)*page_size]
         Snapshot.delete(page_sids)
       end
     end
   end
  end
end

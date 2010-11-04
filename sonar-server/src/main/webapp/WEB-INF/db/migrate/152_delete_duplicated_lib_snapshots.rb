#
# Sonar, entreprise quality control tool.
# Copyright (C) 2009 SonarSource SA
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
class DeleteDuplicatedLibSnapshots < ActiveRecord::Migration
  def self.up
   metric=Metric.find(:first, :conditions => ['name=?','lines'])
   if metric
     snapshots=select_snapshots_without_measures(metric)
     delete_snapshots(snapshots)
   end
  end

  def self.select_snapshots_without_measures(metric)
   snapshots=[]
   say_with_time "Select project snapshots without measures..." do
     snapshots=Snapshot.find_by_sql ["SELECT s.id FROM snapshots s WHERE s.scope='PRJ' and s.qualifier IN ('TRK', 'BRC') AND status='P' AND NOT EXISTS (select m.id from project_measures m WHERE m.snapshot_id=s.id AND m.metric_id=?) and not exists(select * from dependencies d where d.to_snapshot_id=s.id)", metric.id]
   end
   snapshots
  end

  def self.filter_involved_in_dependencies(snapshots)
   result=[]
   if snapshots.size>0
     say_with_time "Filter #{snapshots.size} snapshots..." do
       result=Snapshot.find_by_sql ["SELECT s.id FROM snapshots s where s.id in (?) and not exists(select * from dependencies d where d.to_snapshot_id=s.id", snapshots.map{|s| s.id}]
     end
   end
   return result
  end

  def self.delete_snapshots(snapshots)
   if snapshots.size > 0
     say_with_time "Deleting #{snapshots.size} orphan snapshots..." do
       Snapshot.delete(snapshots.map{|s| s.id})
     end
   end
  end
end

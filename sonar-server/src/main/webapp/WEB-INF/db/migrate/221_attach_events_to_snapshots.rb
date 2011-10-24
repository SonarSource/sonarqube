#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2011 SonarSource
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
# Sonar 2.12
#
class AttachEventsToSnapshots < ActiveRecord::Migration

  def self.up
    Event.find(:all, :conditions => "snapshot_id IS NULL").each do |event|
      next_snapshot = Snapshot.find(:first, :conditions => ["created_at >= ? AND project_id = ?", event.event_date, event.resource_id], :order => :created_at)
      if next_snapshot && (event.category!='Version' || !next_snapshot.event('Version'))
        event.snapshot = next_snapshot
        event.event_date = next_snapshot.created_at
        event.save!
      else
       previous_snapshot = Snapshot.find(:last, :conditions => ["created_at <= ? AND project_id = ?", event.event_date, event.resource_id], :order => :created_at)
       if previous_snapshot && (event.category!='Version' || !previous_snapshot.event('Version'))
         event.snapshot = previous_snapshot
         event.event_date = previous_snapshot.created_at
         event.save!
       end
      end
    end
  end

end

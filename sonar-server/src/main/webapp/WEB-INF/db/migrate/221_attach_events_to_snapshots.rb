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
# Sonar 2.12
#
class AttachEventsToSnapshots < ActiveRecord::Migration

  class Event < ActiveRecord::Base
  end

  class Snapshot < ActiveRecord::Base
  end

  def self.up
    logger = RAILS_DEFAULT_LOGGER
    Event.reset_column_information
    Snapshot.reset_column_information

    Event.find(:all, :conditions => "snapshot_id IS NULL").each do |event|
      begin
        next_snapshot = Snapshot.find(:first, :conditions => ["created_at >= ? AND project_id = ?", event.event_date, event.resource_id], :order => :created_at)
        if next_snapshot && (event.category!='Version' || !has_category?(next_snapshot, 'Version'))
          event.snapshot_id = next_snapshot.id
          event.event_date = next_snapshot.created_at
          event.save
        else
          previous_snapshot = Snapshot.find(:last, :conditions => ["created_at <= ? AND project_id = ?", event.event_date, event.resource_id], :order => :created_at)
          if previous_snapshot && (event.category!='Version' || !has_category?(previous_snapshot,'Version'))
            event.snapshot_id = previous_snapshot.id
            event.event_date = previous_snapshot.created_at
            event.save
          end
        end
      rescue Exception => e
        # do nothing and leave this event as is.
        logger.warn("The following event could not be attached to a snapshot, and therefore won't be used in the future: " + event.name)
      end
    end
  end

  def self.has_category?(snapshot, category)
    Event.exists?({:category => category, :snapshot_id => snapshot.id})
  end
end

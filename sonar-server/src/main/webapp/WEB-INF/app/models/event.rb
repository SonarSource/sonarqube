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
class Event < ActiveRecord::Base

  validates_presence_of    :event_date
  validates_length_of      :name, :within => 1..400
  validates_length_of      :category, :within => 1..50

  belongs_to :resource, :class_name => 'Project', :foreign_key => 'resource_id'
  belongs_to :snapshot

  before_save :populate_snapshot

  def fullname
    if category
      "#{category} #{name}"
    else
      name
    end
  end
  
  # Use this method to display event description, as missing descriptions are not stored in the same way
  # on different DBs (see https://jira.codehaus.org/browse/SONAR-3326)
  def description_text
    description || ''
  end
  
  #
  # For a given snapshot, checks if an event with the same name & category
  # exists in the history of the corresponding resource (= in any existing 
  # processed snapshot for this resource).
  #
  def self.already_exists(snapshot_id, event_name, event_category)
    snapshot = Snapshot.find(snapshot_id.to_i)
    snapshots = Snapshot.find(:all, :conditions => ["status='P' AND project_id=?", snapshot.project_id], :include => 'events')
    snapshots.each do |snapshot|
      snapshot.events.each do |event|
        return true if event.name==event_name && event.category==event_category
      end
    end
    
    return false
  end
  
  #
  # TODO: Remove this code when everything has been checked on the Event handling, both on the UI and the WS API
  #
  def populate_snapshot
    self.snapshot=Snapshot.snapshot_by_date(resource_id, event_date) unless self.snapshot
  end
end

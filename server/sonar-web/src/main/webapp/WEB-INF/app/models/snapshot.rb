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
class Snapshot < ActiveRecord::Base
  include Resourceable

  belongs_to :project, :class_name => 'Project', :foreign_key => 'component_uuid',:primary_key => 'uuid'

  has_many :events, :class_name => 'Event', :foreign_key => 'analysis_uuid', :primary_key => 'uuid', :dependent => :destroy, :order => 'event_date DESC'

  STATUS_UNPROCESSED = 'U'
  STATUS_PROCESSED = 'P'

  def root_project
    project
  end

  def created_at
    long_to_date(:created_at)
  end

  def created_at_long
    read_attribute(:created_at)
  end

  def build_date
    long_to_date(:build_date)
  end
  
  def period1_date
    long_to_date(:period1_date)
  end
  
  def period2_date
    long_to_date(:period2_date)
  end
  
  def period3_date
    long_to_date(:period3_date)
  end
  
  def period4_date
    long_to_date(:period4_date)
  end
  
  def period5_date
    long_to_date(:period5_date)
  end
  
  def long_to_date(attribute)
    date_in_long = read_attribute(attribute)
    Time.at(date_in_long/1000) if date_in_long
  end

  def self.for_timemachine_widget(resource, number_of_columns, options={})
    if number_of_columns == 1
      # Display only the latest snapshot
      return [resource.last_snapshot]
    end

    # Get 1rst & latests snapshots of the period
    snapshot_conditions = ["snapshots.component_uuid=? AND snapshots.status=?", resource.project_uuid, STATUS_PROCESSED]
    if options[:from]
      snapshot_conditions[0] += " AND snapshots.created_at>=?"
      snapshot_conditions << options[:from].to_i * 1000
    end
    first_snapshot = Snapshot.find(:first, :conditions => snapshot_conditions, :order => 'snapshots.created_at ASC')
    last_snapshot = resource.last_analysis

    if first_snapshot==last_snapshot
      return [last_snapshot]
    end

    # Look for the number_of_columns-2 last snapshots to display  (they must have 'Version' events)
    version_snapshots = []
    if number_of_columns > 2
      snapshot_conditions[0] += " AND events.analysis_uuid=snapshots.uuid AND events.category='Version' AND snapshots.id NOT IN (?)"
      snapshot_conditions << [first_snapshot.id, last_snapshot.id]
      version_snapshots=Snapshot.find(:all, :conditions => snapshot_conditions, :include => 'events', :order => 'snapshots.created_at ASC').last(number_of_columns-2)
    end

    return [first_snapshot] + version_snapshots + [last_snapshot]
  end

  def last?
    islast
  end

  def user_events
    categories=EventCategory.categories(true)
    category_names=categories.map { |cat| cat.name }
    Event.find(:all, :conditions => ["analysis_uuid=? AND category IS NOT NULL", uuid], :order => 'event_date desc').select do |event|
      category_names.include?(event.category)
    end
  end

  def event(category)
    result=events.select { |e| e.category==category }
    if result.empty?
      nil
    else
      result.first
    end
  end


  def self.snapshot_by_date(resource_uuid, date)
    if resource_uuid && date
      Snapshot.find(:first, :conditions => ['created_at>=? and created_at<? and component_uuid=?', date.beginning_of_day.to_i*1000, date.end_of_day.to_i*1000, resource_uuid], :order => 'created_at desc')
    else
      nil
    end
  end

  def resource
    project
  end

  def resource_id
    project.id
  end

  def periods?
    (period1_mode || period2_mode || period3_mode || period4_mode || period5_mode) != nil
  end

  def component_uuid_for_authorization
    component_uuid
  end

  def period_mode(period_index)
    send "period#{period_index}_mode"
  end

  def period_param(period_index)
    send "period#{period_index}_param"
  end

  def period_datetime(period_index)
    send "period#{period_index}_date"
  end

  def metrics
    @metrics ||=
      begin
        measures_hash.keys.map { |metric_id| Metric::by_id(metric_id) }.uniq.compact
      end
  end

  # metrics of all the available measures
  def metric_keys
    @metric_keys ||=
      begin
        metrics.map { |m| m.name }
      end
  end

  def has_source
    SnapshotSource.count('id', :conditions => "snapshot_id = #{id}") > 0
  end

  private

  def measures_hash
    @measures_hash ||=
      begin
        hash = {}
        measures.each do |measure|
          hash[measure.metric_id]=measure
        end
        hash
      end
  end

end

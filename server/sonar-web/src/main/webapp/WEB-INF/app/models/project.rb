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
class Project < ActiveRecord::Base
  include Comparable
  include Resourceable

  has_many :snapshots
  has_many :processed_snapshots, :class_name => 'Snapshot', :conditions => "status='#{Snapshot::STATUS_PROCESSED}' AND qualifier<>'LIB'", :order => 'created_at asc'
  has_many :events, :foreign_key => 'component_uuid', :primary_key => 'uuid', :order => 'event_date DESC'
  has_many :project_links, :foreign_key => 'component_uuid', :primary_key => 'uuid', :dependent => :delete_all, :order => 'link_type'
  has_many :user_roles, :foreign_key => 'resource_id'
  has_many :group_roles, :foreign_key => 'resource_id'
  has_many :manual_measures, :foreign_key => 'component_uuid', :primary_key => 'uuid'
  belongs_to :root, :class_name => 'Project', :foreign_key => 'root_id'
  belongs_to :copy_resource, :class_name => 'Project', :foreign_key => 'copy_resource_id'
  belongs_to :person, :class_name => 'Project', :foreign_key => 'person_id'
  has_many :authors, :foreign_key => 'person_id', :dependent => :delete_all
  has_one :index, :class_name => 'ResourceIndex', :foreign_key => 'resource_id', :conditions => 'position=0', :select => 'kee'
  has_many :resource_index, :foreign_key => 'resource_id'

  def self.by_key(k)
    begin
      ki=Integer(k)
      Project.find(ki)
    rescue
      Project.first(:conditions => {:kee => k})
    end
  end

  # return an array with exactly the same size than keys. If an item is not found, then it's nil.
  def self.by_keys(keys)
    keys.map do |key|
      by_key(key)
    end
  end

  def self.delete_resource_tree(project)
    java_facade = Java::OrgSonarServerUi::JRubyFacade.getInstance()
    if project && java_facade.getResourceTypeBooleanProperty(project.qualifier, 'deletable')
      java_facade.deleteResourceTree(project.key)
    end
  end

  def self.root_qualifiers()
    @root_types ||=
      begin
        Java::OrgSonarServerUi::JRubyFacade.getInstance().getResourceRootTypes().map {|type| type.getQualifier()}
      end
  end

  def project
    root||self
  end

  def root_project
    @root_project ||=
      begin
        parent_module(self)
      end
  end

  def modules
    @modules ||=
      begin
        Project.all(:conditions => {:root_id => self.id, :scope => 'PRJ'})
      end
  end

  # bottom-up array of projects,
  def ancestor_projects
    node, nodes = self, []
    nodes << node = node.root while node.root
    nodes
  end

  def resource_link
    @resource_link ||=
      begin
        (copy_resource && copy_resource.qualifier==qualifier) ? copy_resource : nil
      end
  end

  def permanent_resource
    resource_link||self
  end

  def permanent_id
    permanent_resource.id
  end

  def last_snapshot
    @last_snapshot ||=
      begin
        snapshot=Snapshot.first(:conditions => {:islast => true, :project_id => id})
        if snapshot
          snapshot.project=self
        end
        snapshot
      end
  end

  def events_with_snapshot
    events.select { |event| !event.snapshot_id.nil? }
  end

  def key
    kee
  end

  def links
    project_links
  end

  def link(type)
    # to_a avoids conflicts with ActiveRecord:Base.find
    links.to_a.find { |l| l.link_type==type }
  end

  def custom_links
    links.select { |l| l.custom? }
  end

  def standard_links
    links.reject { |l| l.custom? }
  end

  def chart_measures(metric_id)
    sql = Project.send(:sanitize_sql, ['select s.created_at as created_at, m.value as value ' +
                                         ' from project_measures m, snapshots s ' +
                                         ' where s.id=m.snapshot_id and ' +
                                         " s.status='%s' and " +
                                         ' s.project_id=%s and m.metric_id=%s ', 'P', self.id, metric_id]) +
      ' and m.rule_id IS NULL and m.rule_priority IS NULL' +
      ' and m.person_id IS NULL' +
      ' order by s.created_at'
    create_chart_measures(Project.connection.select_all(sql), 'created_at', 'value')
  end

  def <=>(other)
    kee <=> other.kee
  end

  def name(long_if_defined=false)
    if long_if_defined
      long_name || read_attribute(:name)
    else
      read_attribute(:name)
    end
  end

  def fullname
    name
  end

  def branch
    if project? || module?
      s=kee.split(':')
      if s.size>=3
        return s[2]
      end
    end
    nil
  end

  def resource_id_for_authorization
    if library?
      # no security on libraries
      nil
    elsif set?
      self.root_id || self.id
    elsif last_snapshot
      last_snapshot.resource_id_for_authorization
    else
      nil
    end
  end

  def path_name
    last_snapshot && last_snapshot.path_name
  end

  private

  def create_chart_measures(results, date_column_name, value_column_name)
    chart_measures = []
    if results and results.first != nil
      # :sanitize_sql is protected so its behaviour cannot be predicted exactly,
      # the jdbc active record impl adapter returns a db typed objects array
      # when regular active record impl return string typed objects
      if results.first[date_column_name].class == Time
        results.each do |hash|
          chart_measures << ChartMeasure.new(hash[date_column_name], hash[value_column_name])
        end
      else
        results.each do |hash|
          chart_measures << ChartMeasure.new(Time.parse(hash[date_column_name]), hash[value_column_name].to_d)
        end
      end
    end
    chart_measures
  end

  def parent_module(current_module)
    current_module.root ? parent_module(current_module.root) : current_module
  end

end

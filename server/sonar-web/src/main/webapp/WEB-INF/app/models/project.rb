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

  has_many :events, :foreign_key => 'component_uuid', :primary_key => 'uuid', :order => 'event_date DESC'
  has_many :project_links, :foreign_key => 'component_uuid', :primary_key => 'uuid', :dependent => :delete_all, :order => 'link_type'
  belongs_to :root, :class_name => 'Project', :foreign_key => 'root_uuid', :primary_key => 'uuid'
  belongs_to :copy_resource, :class_name => 'Project', :foreign_key => 'copy_component_uuid', :primary_key => 'uuid'
  belongs_to :person, :class_name => 'Project', :foreign_key => 'developer_uuid', :primary_key => 'uuid'
  has_many :authors, :foreign_key => 'person_id', :dependent => :delete_all
  has_one :index, :class_name => 'ResourceIndex', :foreign_key => 'component_uuid', :primary_key => 'uuid', :conditions => 'position=0', :select => 'kee'
  has_many :resource_index, :foreign_key => 'resource_id'
  has_one :last_analysis, :class_name => 'Snapshot', :foreign_key => 'component_uuid', :primary_key => 'project_uuid', :conditions => ['snapshots.islast=?', true]

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

  def self.root_qualifiers()
    @root_types ||=
      begin
        Java::OrgSonarServerUi::JRubyFacade.getInstance().getResourceRootTypes().map {|type| type.getQualifier()}
      end
  end

  def project
    root||self
  end

  def root?
    project_uuid == uuid
  end

  def root_project
    @root_project ||=
      begin
        if project_uuid == uuid
          self
        else
          Project.find(:first, :conditions => ['uuid = ?', project_uuid])
        end
      end
  end

  def modules
    @modules ||=
      begin
        Project.all(:conditions => ['root_uuid=? and uuid <> ? and scope=?', self.uuid, self.uuid, 'PRJ'])
      end
  end

  # bottom-up array of projects,
  def ancestor_projects
    node, nodes = self, []
    nodes << node = node.root while node.root_uuid != node.uuid
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
        analysis = Snapshot.find(:first, :conditions => ['component_uuid = ? and islast = ?', project_uuid, true])
        if analysis
           ComponentSnapshot.new(analysis, self)
         else
           nil
         end
      end
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

  def component_uuid_for_authorization
    self.project_uuid
  end

  private

  def parent_module(current_module)
    current_module.root.uuid = current_module.uuid ? current_module : parent_module(current_module.root)
  end

end

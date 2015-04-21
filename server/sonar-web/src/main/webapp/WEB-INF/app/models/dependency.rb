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
class Dependency < ActiveRecord::Base
  belongs_to :from, :class_name => 'Project', :foreign_key => 'from_component_uuid', :primary_key => 'uuid'
  belongs_to :from_snapshot, :class_name => 'Snapshot', :foreign_key => 'from_snapshot_id'

  belongs_to :to, :class_name => 'Project', :foreign_key => 'to_component_uuid', :primary_key => 'uuid'
  belongs_to :to_snapshot, :class_name => 'Snapshot', :foreign_key => 'to_snapshot_id'

  belongs_to :project, :class_name => 'Project', :foreign_key => 'project_id'
  belongs_to :project_snapshot, :class_name => 'Snapshot', :foreign_key => 'project_snapshot_id' 

  def usage
    dep_usage
  end

  def weight
    dep_weight
  end

  def to_json(options={})
    hash={:id => id, :fi => from.id, :ti => to.id}
    hash[:u]=usage if usage
    hash[:w]=weight if weight
    hash[:fk]=from.key
    hash[:fn]=from.name(true)
    hash[:fq]=from.qualifier
    hash[:tk]=to.key
    hash[:tn]=to.name(true)
    hash[:tq]=to.qualifier
    hash
  end

  def to_xml(xml, options={})
    xml.id(id)
    xml.fi(from.id)
    xml.ti(to.id)
    xml.u(usage) if usage
    xml.w(weight) if weight
    xml.fk(from.key)
    xml.fn(from.name(true))
    xml.fq(from.qualifier)
    xml.tk(to.key)
    xml.tn(to.name(true))
    xml.tq(to.qualifier)
    xml
  end
end

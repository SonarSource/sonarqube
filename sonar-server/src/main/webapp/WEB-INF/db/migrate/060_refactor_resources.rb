#
# Sonar, open source software quality management tool.
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
class RefactorResources < ActiveRecord::Migration

  def self.up
    add_column(:projects, :language, :string, :null => true, :limit => 5)
    Project.reset_column_information

    Snapshot060.update_all("scope='FIL', qualifier='CLA'", "scope='CLA' and qualifier='JMA' and status='P'")
    Snapshot060.update_all("scope='FIL', qualifier='UTL'", "scope='CLA' and qualifier='JUN' and status='P'")
    Snapshot060.update_all("scope='DIR', qualifier='PAC'", "scope='PAC' and qualifier='JAV' and status='P'")
    Snapshot060.update_all("scope='PRJ', qualifier='TRK'", "root_snapshot_id is null and scope='PRJ' and qualifier='JAV' and status='P'")
    Snapshot060.update_all("scope='PRJ', qualifier='BRC'", "root_snapshot_id is not null and scope='PRJ' and qualifier='JAV' and status='P'")
    Snapshot060.update_all("scope='FIL', qualifier='FIL'", "scope='CLA' and qualifier='PLS' and status='P'")
    Snapshot060.update_all("scope='DIR', qualifier='DIR'", "scope='PAC' and qualifier='PLS' and status='P'")

    Project060.update_all("scope='FIL', qualifier='CLA', language='java'", "scope='CLA' and qualifier='JMA'")
    Project060.update_all("scope='FIL', qualifier='UTS', language='java'", "scope='CLA' and qualifier='JUN'")
    Project060.update_all("scope='FIL', qualifier='FIL', language='plsql'", "scope='CLA' and qualifier='PLS'")
    Project060.update_all("scope='DIR', qualifier='PAC', language='java'", "scope='PAC' and qualifier='JAV'")
    Project060.update_all("scope='DIR', qualifier='DIR', language='plsql'", "scope='PAC' and qualifier='PLS'")
    Project060.update_all("scope='PRJ', qualifier='TRK', language='java'", "scope='PRJ' and qualifier='JAV'")
    Project060.update_all("scope='PRJ', qualifier='TRK', language='plsql'", "scope='PRJ' and qualifier='PLS'")

    Snapshot060.find(:all, :conditions => {:scope => 'PRJ'}).each do |snapshot|
      project=Project060.find(snapshot.project_id)
      if project
        project.qualifier=(snapshot.root_snapshot_id ? 'BRC' : 'TRK')
        project.save
      end
    end

    update_path_and_depth
  end

  def self.down

  end

  def self.update_path_and_depth
    say_with_time "Updating snapshots on depth 0..." do
      Snapshot060.update_all("depth=0, path=''", "root_snapshot_id is null")
    end
    update_path_and_depth_for_depth(1)
    update_path_and_depth_for_depth(2)
    update_path_and_depth_for_depth(3)
    update_path_and_depth_for_depth(4)
    update_path_and_depth_for_depth(5)
    update_path_and_depth_for_depth(6)
  end

  def self.update_path_and_depth_for_depth(depth)
    parents=Snapshot060.find(:all, :conditions => {:depth => depth - 1})
    say_with_time "Updating #{parents.size} snapshots on depth #{depth}..." do
      parents.each do |parent|
        children=Snapshot060.find(:all, :conditions => {:parent_snapshot_id => parent.id}).each do |child|
          child.update_attributes(:depth => depth, :path => "#{parent.path}#{parent.id}.")
        end
      end
    end
  end

  class Snapshot060 < ActiveRecord::Base
    set_table_name 'snapshots'
  end

  class Project060 < ActiveRecord::Base
    set_table_name 'projects'
  end
end

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
class EnsureMeasureSnapshotColumnNull < ActiveRecord::Migration

  def self.up
    # due to a migration issue under oracle with previous version, we have to make sure that this colum
    # is set to null even if already done in migration 49 
    begin
      change_column(:project_measures, :snapshot_id, :integer, :null => true)
    rescue
      puts "project_measures.snapshot_id already set to nullable"
    end

  end

  def self.down

  end

end
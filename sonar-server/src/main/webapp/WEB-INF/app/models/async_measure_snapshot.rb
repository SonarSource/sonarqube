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
class AsyncMeasureSnapshot < ActiveRecord::Base
   
  belongs_to :async_measure, :foreign_key => 'project_measure_id', :class_name => "ProjectMeasure"
  belongs_to :snapshot

  def self.search(sids, metric_ids=nil)
    sql='async_measure_snapshots.snapshot_id IN (:sids)'
    hash={:sids => sids}
    if metric_ids
      sql+=' AND async_measure_snapshots.metric_id IN (:mids)'
      hash[:mids]=metric_ids
    end
    async_measures=AsyncMeasureSnapshot.find(:all,
        :include => ['async_measure'],
        :conditions => [sql, hash])

    result=[]
    async_measures.each do |am|
      clone=am.async_measure.clone
      clone.snapshot_id=am.snapshot_id
      result<<clone
    end
    result
  end

  def measure
    async_measure
  end
end
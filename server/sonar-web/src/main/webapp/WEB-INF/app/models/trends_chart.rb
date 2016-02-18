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
class TrendsChart

  def self.time_machine_measures(resource, metric_ids, options={})
    unless metric_ids.empty?
      sql= "select s.created_at as created_at, m.value as value, m.metric_id as metric_id, s.id as sid " +
            " from project_measures m LEFT OUTER JOIN snapshots s ON s.id=m.snapshot_id " +
            " where m.rule_id is null " +
            " and s.status=? " +
            " and s.project_id=? " +
            " and m.metric_id in (?) " +
            " and m.rule_priority is null and m.person_id is null"
      if (options[:from])
        sql += ' and s.created_at>=?'
      end
      if (options[:to])
        sql += ' and s.created_at<=?'
      end
      sql += ' order by s.created_at ASC'
      conditions=[sql, Snapshot::STATUS_PROCESSED, resource.id, metric_ids]
      if (options[:from])
        conditions<<options[:from].to_i*1000
      end
      if (options[:to])
        conditions<<options[:to].to_i*1000
      end
      ProjectMeasure.connection.select_all(Project.send(:sanitize_sql, conditions))
    end
  end

end

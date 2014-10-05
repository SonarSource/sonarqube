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

#
# SonarQube 4.5.1
#
class MysqlMediumtextToLongtext < ActiveRecord::Migration

  def self.up
    if dialect()=='mysql'
      apply 'rules', 'description'
      apply 'rules', 'note_data'
      apply 'snapshot_sources', 'data'
      apply 'properties', 'text_value'
      apply 'measure_filters', 'data'
      apply 'graphs', 'data'
      apply 'snapshot_data', 'snapshot_data'
      apply 'issue_changes', 'change_data'
      apply 'issue_filters', 'data'
      apply 'activities', 'data_field'
    end
  end

  def self.apply(table, column)
    ActiveRecord::Base.connection.execute("alter table #{table} modify #{column} longtext")
  end
end

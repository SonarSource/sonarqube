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
class Api::DashboardConfiguration

  attr_accessor :dashboard, :period_index, :selected_period, :snapshot

  def initialize(dashboard, options={})
    @dashboard=dashboard
    @period_index=(options[:period_index].to_i>0 ? options[:period_index].to_i : nil)
    @selected_period=(@period_index != nil)
    @snapshot=options[:snapshot]
  end

  def name
    @dashboard.name
  end

  def description
    @dashboard.description
  end

  def layout
    @dashboard.column_layout
  end

  def number_of_columns
    @dashboard.number_of_columns
  end

  def selected_period?
    @selected_period
  end

  def from_datetime
    if selected_period? && @snapshot
      @snapshot.period_datetime(@period_index)
    else
      nil
    end
  end
end

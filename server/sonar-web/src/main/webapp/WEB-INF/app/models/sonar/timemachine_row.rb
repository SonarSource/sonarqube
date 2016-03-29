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
class Sonar::TimemachineRow
  attr_accessor :metric

  def initialize(metric)
    @metric=metric
    @measure_by_sid={}
  end

  def add_measure(measure)
    @measure_by_sid[measure.snapshot_id]=measure
  end

  def measure(snapshot)
    @measure_by_sid[snapshot.id]
  end

  def domain
    @metric.domain.nil? ? "" : @metric.domain
  end

  def <=>(other)
    (self.domain <=> other.domain).nonzero? || (self.metric.short_name <=> other.metric.short_name)
  end

  def sparkline
    if metric.numeric? && @measure_by_sid.size > 1
      x = []
      y = []
      @measure_by_sid.values.each do |measure|
        # date.to_f does not works under oracle
        x << measure.snapshot.created_at.to_s(:number)
        y << (measure.value.nil? ? 0 : measure.value)
      end
      [x, y]
    else
      nil
    end
  end
end

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
    @measures_by_analysis_uuid = {}
  end

  def add_measure(measure)
    @measures_by_analysis_uuid[measure.analysis_uuid] = measure
  end

  def measure(analysis)
    @measures_by_analysis_uuid[analysis.uuid]
  end

  def domain
    @metric.domain.nil? ? "" : @metric.domain
  end

  def <=>(other)
    (self.domain <=> other.domain).nonzero? || (self.metric.short_name <=> other.metric.short_name)
  end

  def sparkline(analyses)
    if metric.numeric? && @measures_by_analysis_uuid.size > 1
      x = []
      y = []
      analyses.each do |analysis|
        m = measure(analysis)
        if m
          # date.to_f does not works under oracle
          x << analysis.created_at.to_s(:number)
          y << (m.value.nil? ? 0 : m.value)
        end
      end
      [x, y]
    else
      nil
    end
  end
end

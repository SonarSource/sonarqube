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
module ResourceHelper
  DUPLICATION_SNIPPET_DEFAULT_NB_OF_LINES = 6

  def format(new, prefix, measure_name, options = {})
    if new
      key = 'new_' + prefix + measure_name
      value = format_variation(measure(key), :period => @period, :default => '-', :style => 'none')
    else
      key = prefix + measure_name
      value = measure(key) ? measure(key).formatted_value : '0'
    end

    if options[:span]
      "<span id=\"m_#{key}\">#{value}</span>"
    else
      value
    end
  end

  def format_ratio(new, prefix, measure_name1, measure_name2)
    value1 = format(new, prefix, measure_name1)
    value2 = format(new, prefix, measure_name2)

    '(' + (value1.to_i - value2.to_i).to_s + '/' + value1 + ')'
  end

end

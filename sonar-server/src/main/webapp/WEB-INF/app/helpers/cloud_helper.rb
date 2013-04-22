#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2013 SonarSource
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
module CloudHelper
  MIN_SIZE_PERCENT=60.0
  MAX_SIZE_PERCENT=240.0
  
  def font_size(value)
    divisor=@max_size_value - @min_size_value
    size=MIN_SIZE_PERCENT
    if divisor!=0.0
      multiplier=(MAX_SIZE_PERCENT - MIN_SIZE_PERCENT)/divisor
      size=MIN_SIZE_PERCENT + ((@max_size_value - (@max_size_value-(value - @min_size_value)))*multiplier)
    end
    size.to_i
  end
end

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
class MeasureFilterDisplay

  DISPLAY_CLASSES = [MeasureFilterDisplayList, MeasureFilterDisplayTreemap]

  def self.create(filter, options)
    key = filter.criteria('display')
    display_class=DISPLAY_CLASSES.find{|display_class| display_class::KEY==key.to_sym} if key
    display_class.new(filter, options) if display_class
  end

  def self.keys
    DISPLAY_CLASSES.map{|display_class| display_class::KEY}
  end

  def key
    self.class::KEY
  end

  attr_reader :filter, :options

  def initialize(filter, options)
    @filter = filter
    @options = options

    if filter.base_resource
      qualifiers = filter.criteria(:qualifiers)
      filter.set_criteria_value(:onBaseComponents, 'true') unless qualifiers && !qualifiers.empty?
    end
  end

  # sorted array of parameters :
  # [[key1,value1], [key2,value2]]
  def url_params
    []
  end
end

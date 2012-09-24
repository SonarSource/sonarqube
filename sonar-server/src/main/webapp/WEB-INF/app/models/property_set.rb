#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
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
# License along with {library}; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
class PropertySet < ActiveRecord::Base
  attr_accessor :name

  def self.columns
    @columns ||= [];
  end

  def self.column(name, sql_type = nil, default = nil, null = true)
    columns << ActiveRecord::ConnectionAdapters::Column.new(name.to_s, default, sql_type.to_s, null)
  end

  def self.findAll(set_name)
    ActiveSupport::JSON.decode(values_as_json(set_name)).map { |set| PropertySet.new(set) }
  end

  def save(validate = true)
    validate ? valid? : true
  end

  private

  def self.values_as_json(set_name)
    json = Property.value('sonar.property_set.' + set_name)

    #json || '[]'
    json || '[{"name":"set1"},{"name":"set2"}]'
  end
end

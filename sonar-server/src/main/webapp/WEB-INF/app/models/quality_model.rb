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
class QualityModel < ActiveRecord::Base

  validates_length_of :name, :within => 1..100
  validates_uniqueness_of :name

  has_many :characteristics, :dependent => :delete_all

  def root_characteristics(only_enabled=true)
    @roots ||=
      begin
        characteristics.select do |c|
          c.parents.empty? && (!only_enabled || c.enabled)
        end
      end
  end

  def characteristics_with_rule(only_enabled=true)
    @characteristics_with_rule ||=
      begin
        characteristics.select do |c|
          (!c.rule_id.nil?) && (!only_enabled || c.enabled)
        end
      end
  end

  def characteristics_without_rule(only_enabled=true)
    @characteristics_without_rule ||=
      begin
        characteristics.select do |c|
          c.rule_id.nil? && (!only_enabled || c.enabled)
        end
      end
  end

  # be careful, can return disabled characteristic
  def characteristic(id)
    @characteristics_by_id ||=
        begin
          hash={}
          characteristics.each {|c| hash[c.id]=c}
          hash
        end
    @characteristics_by_id[id]
  end
end
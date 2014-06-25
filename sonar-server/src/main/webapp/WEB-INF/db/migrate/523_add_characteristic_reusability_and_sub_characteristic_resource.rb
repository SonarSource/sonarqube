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
# SonarQube 4.3
# SONAR-5180
#
class AddCharacteristicReusabilityAndSubCharacteristicResource < ActiveRecord::Migration

  class Characteristic < ActiveRecord::Base
  end

  def self.up
    Characteristic.reset_column_information

    # On an empty DB, there are no characteristics, they're all gonna be created after
    if Characteristic.all.size > 0
      create_characteristic_reusability_and_its_sub_characteristics
      create_sub_characteristic_resource
    end
  end

  def self.create_characteristic_reusability_and_its_sub_characteristics
    # 'Reusability' is the new characteristic, it has two sub characteristics : 'Modularity' and 'Transportability'
    reusability = Characteristic.first(:conditions => ['kee=? AND enabled=?', 'REUSABILITY', true])
    modularity = Characteristic.first(:conditions => ['kee=? AND enabled=?', 'MODULARITY', true])
    transportability = Characteristic.first(:conditions => ['kee=? AND enabled=?', 'TRANSPORTABILITY', true])

    unless reusability
      # Reusability should become the first characteristic
      reusability = Characteristic.create(:name => 'Reusability', :kee => 'REUSABILITY', :characteristic_order => 1, :enabled => true)
      # So all other characteristics have to moved down
      Characteristic.all(:conditions => ['enabled=? AND parent_id IS NULL AND rule_id IS NULL', true]).each do |c|
        c.characteristic_order = c.characteristic_order + 1
        c.save!
      end
    end

    Characteristic.create(:name => 'Modularity', :kee => 'MODULARITY', :parent_id => reusability.id, :enabled => true) unless modularity
    Characteristic.create(:name => 'Transportability', :kee => 'TRANSPORTABILITY', :parent_id => reusability.id, :enabled => true) unless transportability
  end

  def self.create_sub_characteristic_resource
    # New sub characteristic 'Resource' (under characteristic 'Reliability')
    resource = Characteristic.first(:conditions => ['kee=? AND enabled=?', 'RESOURCE_RELIABILITY', true])
    unless resource
      reliability = Characteristic.first(:conditions => ['kee=? AND enabled=?', 'RELIABILITY', true])
      Characteristic.create(:name => 'Resource', :kee => 'RESOURCE_RELIABILITY', :parent_id => reliability.id, :enabled => true) if reliability
    end
  end

end

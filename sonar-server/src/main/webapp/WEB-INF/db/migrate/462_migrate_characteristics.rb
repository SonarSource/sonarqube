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

#
# Sonar 4.1
# SONAR-4831
# SONAR-4895
#
class MigrateCharacteristics < ActiveRecord::Migration

  class QualityModel < ActiveRecord::Base
  end

  class Characteristic < ActiveRecord::Base
  end

  class CharacteristicEdge < ActiveRecord::Base
  end

  class CharacteristicProperty < ActiveRecord::Base
  end

  def self.up
    now = Time.now

    QualityModel.reset_column_information
    Characteristic.reset_column_information
    CharacteristicEdge.reset_column_information
    CharacteristicProperty.reset_column_information

    sqale_model = QualityModel.first(:conditions => ['name=?', 'SQALE'])
    characteristics = Characteristic.all(:conditions => ['quality_model_id=?', sqale_model.id])
    characteristic_edges = CharacteristicEdge.all
    parent_ids_by_characteristic_id = {}
    characteristic_edges.each do |edge|
      parent_ids_by_characteristic_id[edge.child_id] = edge.parent_id
    end

    properties = CharacteristicProperty.all
    properties_by_characteristic_id = {}
    properties.each do |prop|
      char_properties = properties_by_characteristic_id[prop.characteristic_id] || []
      char_properties << prop
      properties_by_characteristic_id[prop.characteristic_id] = char_properties
    end

    requirements_to_delete = []

    characteristics.each do |characteristic|
      # Requirement
      if characteristic.rule_id
        char_properties = properties_by_characteristic_id[characteristic.id]
        function = char_properties.find { |prop| prop.kee == 'remediationFunction'} if char_properties
        if char_properties && function
          factor = char_properties.find { |prop| prop.kee == 'remediationFactor' }
          offset = char_properties.find { |prop| prop.kee == 'offset' }

          case function.text_value
            when 'linear'
              characteristic.function_key = 'linear'
              characteristic.factor_value = factor.value
              characteristic.factor_unit = factor.text_value
              characteristic.offset_value = 0.0
              characteristic.offset_unit = 'mn'

            when 'linear_offset'
              characteristic.function_key = 'linear_offset'
              characteristic.factor_value = factor.value
              characteristic.factor_unit = factor.text_value
              characteristic.offset_value = offset.value
              characteristic.offset_unit = offset.text_value

            # linear_threshold is depreciated and is replaced by linear
            when 'linear_threshold'
              characteristic.function_key = 'linear'
              characteristic.factor_value = factor.value
              characteristic.factor_unit = factor.text_value
              characteristic.offset_value = 0.0
              characteristic.offset_unit = 'mn'

            # constant_resource is no more managed anymore, it has to be deleted
            when 'constant_resource'
              requirements_to_delete << characteristic
          end
          # requirement without properties or without remediationFunction has to be deleted
        else
          requirements_to_delete << characteristic
        end
      end

      characteristic.parent_id = parent_ids_by_characteristic_id[characteristic.id]
      characteristic.created_at = now
      characteristic.updated_at = now
      characteristic.save
    end

    requirements_to_delete.each do |requirement|
      CharacteristicProperty.delete_all(['characteristic_id=?', requirement.id])
      CharacteristicEdge.delete_all(['child_id=?', requirement.id])
      requirement.delete
    end
  end

end


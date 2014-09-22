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
# Sonar 4.1
# SONAR-4831
# SONAR-4891
#

class RemoveUselessCharacteristicsData < ActiveRecord::Migration

  class QualityModel < ActiveRecord::Base
  end

  class Characteristic < ActiveRecord::Base
  end

  def self.up
    Characteristic.reset_column_information

    # Delete all characteristics not related to SQALE
    sqale_model = QualityModel.first(['name = ?', 'SQALE'])
    if sqale_model
      Characteristic.delete_all(['quality_model_id <> ?', sqale_model.id.to_i])
    end

    # Delete useless columns
    remove_column('characteristics', 'quality_model_id')
    remove_column('characteristics', 'depth')
    remove_column('characteristics', 'description')

    # Delete useless tables
    drop_table(:characteristic_properties)
    drop_table(:characteristic_edges)
    drop_table(:quality_models)
  end

end

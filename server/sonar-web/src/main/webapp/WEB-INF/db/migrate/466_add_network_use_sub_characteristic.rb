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
# SonarQube 4.1
# SONAR-4870
#

class AddNetworkUseSubCharacteristic < ActiveRecord::Migration

  class Characteristic < ActiveRecord::Base
  end

  def self.up
    Characteristic.reset_column_information

    # On an empty DB, there are no characteristics, they're all gonna be created after (so new characteristics has always to be also added in the technical-debt-model.xml file)
    if Characteristic.all.size > 0
      efficiency = Characteristic.first(:conditions => ['kee=? AND enabled=?', 'EFFICIENCY', true])
      network_use = Characteristic.first(:conditions => ['name=? AND enabled=?', 'Network use ', true])

      # The Efficiency characteristic can have been deleted from the SQALE plugin
      # And the Network use can already been created
      if efficiency && !network_use
        Characteristic.create(:kee => 'NETWORK_USE_EFFICIENCY', :name => 'Network use', :enabled => true, :parent_id => efficiency.id, :root_id => efficiency.id) if efficiency
      end
    end
  end

end

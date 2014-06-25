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
class UpdateSubCharacteristicNetworkUseKey < ActiveRecord::Migration

  class Characteristic < ActiveRecord::Base
  end

  def self.up
    Characteristic.reset_column_information

    # On an empty DB, there are no characteristics, they're all gonna be created after
    if Characteristic.all.size > 0
      # NETWORK_USE was created with a bad key in the migration 466
      network_use = Characteristic.first(:conditions => ['kee=? AND enabled=?', 'NETWORK_USE_EFFICIENCY', true])
      if network_use
        network_use.kee = 'NETWORK_USE'
        network_use.save!
      end
    end
  end

end

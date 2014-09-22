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
#
class ConvertAlertsToQualityGates < ActiveRecord::Migration

  class RulesProfile < ActiveRecord::Base
  end

  class Alert < ActiveRecord::Base
  end

  class QualityGate < ActiveRecord::Base
  end

  class QualityGateCondition < ActiveRecord::Base
  end

  def self.up

    RulesProfile.reset_column_information
    Alert.reset_column_information
    QualityGate.reset_column_information
    QualityGateCondition.reset_column_information

    alerts = {}
    Alert.all.each do |alert|
      alerts[alert.profile_id] ||= []
      alerts[alert.profile_id].push alert
    end

    new_operators = {
      '='  => 'EQ',
      '!=' => 'NE',
      '>'  => 'GT',
      '<'  => 'LT'
    }

    RulesProfile.all.each do |profile|
      if alerts.has_key?(profile.id)
        qgate_name = profile.name + ' - ' + profile.language

        # This block allows re-execution in case of failure
        old_qgate = QualityGate.find_by_name(qgate_name)
        unless old_qgate.nil?
          QualityGateCondition.destroy_all(:qgate_id => old_qgate.id)
          old_qgate.destroy
        end

        qgate = QualityGate.create(:name => qgate_name)
        alerts[profile.id].each do |alert|
          QualityGateCondition.create(:qgate_id => qgate.id, :metric_id => alert.metric_id, :operator => new_operators[alert.operator],
            :value_warning => alert.value_warning, :value_error => alert.value_error, :period => alert.period)
        end
      end
    end
  end

end

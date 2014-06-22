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
# SonarQube 4.4
# SONAR-5384
#
class AddUniqueIndexOnActiveRuleKey < ActiveRecord::Migration


  class ActiveRuleParameter < ActiveRecord::Base
  end

  class ActiveRule < ActiveRecord::Base
  end

  def self.up
    ActiveRule.reset_column_information
    ActiveRuleParameter.reset_column_information

    # Search for all rules activated many times on a same profile
    rule_actived_many_times_on_same_profile = ActiveRule.all(
        :select => 'rule_id,profile_id',
        :group => 'rule_id,profile_id',
        :having => 'COUNT(*) > 1'
    )

    rule_actived_many_times_on_same_profile.each do |duplicate_active_rule|
      # Search for all duplication on current rule and profile
      active_rules = ActiveRule.all(
          :conditions => {:rule_id => duplicate_active_rule.rule_id, :profile_id => duplicate_active_rule.profile_id}
      )
      # Remove duplication, keep only one active rule (first one)
      active_rules.drop(1).each do |active_rule|
        ActiveRuleParameter.delete_all(:active_rule_id => active_rule.id)
        active_rule.delete
      end
    end

    add_index :active_rules, [:profile_id, :rule_id], :name => 'uniq_profile_rule_ids', :unique => true
  end

end

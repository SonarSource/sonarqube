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
# SONAR-5007
#
class DropRuleTagsTables < ActiveRecord::Migration

  class Rule < ActiveRecord::Base
  end

  class Tag < ActiveRecord::Base
    set_table_name 'rule_tags'
  end

  class RuleTag < ActiveRecord::Base
    set_table_name 'rules_rule_tags'
  end

  def self.up
    # load tags
    tags_by_id={}
    Tag.find(:all).inject(tags_by_id) do |h, tag|
      h[tag.id]=tag.tag
      h
    end

    # load associations between rules and tags
    rule_tags_by_rule_id={}
    RuleTag.find(:all).inject(rule_tags_by_rule_id) do |h, rule_tag|
      h[rule_tag.rule_id]||=[]
      h[rule_tag.rule_id]<<rule_tag
      h
    end

    # move tags to RULES.TAGS and RULES.SYSTEM_TAGS (see migration 533)
    rule_tags_by_rule_id.each do |rule_id, rule_tags|
      rule=Rule.find(rule_id)
      if rule
        system_tags=[]
        user_tags=[]
        rule_tags.each do |rule_tag|
          if rule_tag.tag_type=='SYSTEM'
            system_tags<<tags_by_id[rule_tag.rule_tag_id]
          else
            user_tags<<tags_by_id[rule_tag.rule_tag_id]
          end
        end
        rule.tags=user_tags.join(',')
        rule.system_tags=system_tags.join(',')
        rule.save!
      end
    end

    drop_table(:rules_rule_tags)
    drop_table(:rule_tags)
  end

end

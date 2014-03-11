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
# SonarQube 4.2
#
class AddRuleTags < ActiveRecord::Migration

  def self.up
    create_table :rule_tags do |t|
      t.column :tag,                :string,      :null => true,    :limit => 100
    end
    create_table :rules_rule_tags do |t|
      t.column :rule_id,            :integer,     :null => false
      t.column :rule_tag_id,        :integer,     :null => false
      t.column :tag_type,           :string,      :null => true,    :limit => 20
    end
    add_index 'rules_rule_tags', ['rule_id', 'rule_tag_id'], :unique => true, :name => 'uniq_rule_tags'
  end

end

#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2016 SonarSource
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
class Profile < ActiveRecord::Base
  set_table_name 'rules_profiles'

  has_many :active_rules, :class_name => 'ActiveRule', :foreign_key => 'profile_id', :dependent => :destroy, :include => ['rule']

  after_initialize :readonly!

  def active?
    active
  end

  def key
    "#{language}_#{name}"
  end

  def default_profile?
    Property.value("sonar.profile.#{language}")==name
  end

  def to_hash_json
    {
      :name => name,
      :language => language,
      :version => version,
      :rules => active_rules.map { |active| active.rule.to_hash_json(self, active) }
    }
  end

  def self.by_default(language)
    default_name = Property.value("sonar.profile.#{language}")
    default_name.present? ? Profile.first(:conditions => {:name => default_name, :language => language}) : nil
  end

  def self.find_by_name_and_language(name, language)
    Profile.first(:conditions => {:name => name, :language => language})
  end
end

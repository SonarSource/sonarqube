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
class ActiveRuleNote < ActiveRecord::Base
  belongs_to :active_rule
  alias_attribute :text, :data
  
  validates_presence_of :active_rule, :message => "can't be empty"
  validates_presence_of :user_login, :message => "can't be empty"
  validates_length_of :data, :minimum => 1

  def user
    @user ||=
        begin
          user_login ? User.first(:conditions => ['login=?', user_login]) : nil
        end
  end

  def html_text
    Api::Utils.markdown_to_html(text)
  end

  def plain_text
    Api::Utils.convert_string_to_unix_newlines(text)
  end
  
end

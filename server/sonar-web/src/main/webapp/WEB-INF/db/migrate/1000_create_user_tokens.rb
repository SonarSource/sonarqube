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
# SonarQube 5.3
# SONAR-7039
#
class CreateUserTokens < ActiveRecord::Migration

  def self.up
    create_table 'user_tokens' do |t|
      t.column 'login', :string, :limit => 255, :null => false
      t.column 'name', :string, :limit => 100, :null => false
      t.column 'token_hash', :string, :limit => 255, :null => false
      t.column 'created_at', :big_integer, :null => false
    end
    add_index 'user_tokens', 'token_hash', :name => 'user_tokens_token_hash', :unique => true
    add_index 'user_tokens', ['login','name'], :name => 'user_tokens_login_name', :unique => true
  end

end


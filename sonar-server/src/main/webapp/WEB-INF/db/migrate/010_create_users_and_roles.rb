 #
 # Sonar, entreprise quality control tool.
 # Copyright (C) 2009 SonarSource SA
 # mailto:contact AT sonarsource DOT com
 #
 # Sonar is free software; you can redistribute it and/or
 # modify it under the terms of the GNU Lesser General Public
 # License as published by the Free Software Foundation; either
 # version 3 of the License, or (at your option) any later version.
 #
 # Sonar is distributed in the hope that it will be useful,
 # but WITHOUT ANY WARRANTY; without even the implied warranty of
 # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 # Lesser General Public License for more details.
 #
 # You should have received a copy of the GNU Lesser General Public
 # License along with Sonar; if not, write to the Free Software
 # Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 #
class CreateUsersAndRoles < ActiveRecord::Migration

  def self.up
    create_users
  end

  def self.down
    drop_table 'users'
  end
  
  private 
  
  def self.create_users
    create_table 'users' do |t|
      t.column :login,                     :string, :limit => 40
      t.column :name,                      :string, :limit => 200, :null => true
      t.column :email,                     :string, :limit => 100
      t.column :crypted_password,          :string, :limit => 40
      t.column :salt,                      :string, :limit => 40
      t.column :created_at,                :datetime
      t.column :updated_at,                :datetime
      t.column :remember_token,            :string, :limit => 500, :null => true
      t.column :remember_token_expires_at, :datetime
    end
    
    User.create(:login => 'admin', :name => 'Administrator', :email => '', :password => 'admin',
      :password_confirmation => 'admin')
  end

end

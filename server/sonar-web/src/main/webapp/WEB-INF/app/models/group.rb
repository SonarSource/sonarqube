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
class Group < ActiveRecord::Base

  ANYONE = 'anyone'

  has_and_belongs_to_many :users, :uniq => true
  has_many :group_roles, :dependent => :delete_all
  
  validates_presence_of     :name
  validates_length_of       :name,    :within => 1..255
  validates_length_of       :description,    :maximum => 200, :allow_blank => true
  validates_uniqueness_of   :name
  validate       :name_cant_be_anyone

  # all the users that are NOT members of this group
  def available_users
    User.find(:all, :conditions => ["active=?", true], :order => 'name') - users
  end

  def set_users(new_users=[])
    self.users.clear
    
    new_users=(new_users||[]).compact.uniq
    self.users = User.find(new_users)
    save
  end

  def <=>(other)
    return -1 if name.nil?
    return 1 if other.nil? || other.name.nil?
    name.downcase<=>other.name.downcase
  end

  def name_cant_be_anyone
    errors.add(:name, 'cannot be "Anyone" as this is a reserved group name.') if name && name.downcase == ANYONE
  end
end

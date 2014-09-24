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
# Sonar 3.4
#
class CreateSemaphores < ActiveRecord::Migration

  def self.up

    # MySQL does not manage unique indexes on columns with more than 767 characters
    # A letter can be encoded with up to 3 characters (it depends on the db charset),
    # so unique indexes are allowed only on columns with less than 767/3=255 characters.
    # For this reason the checksum of semaphore name is computed and declared as unique.
    # There are no constraints on the semaphore name itself.
    create_table :semaphores do |t|
      t.string :name, :limit => 4000, :null => false
      t.string :checksum, :limit => 200, :null => false
      t.datetime :locked_at
      t.timestamps
    end
    add_index :semaphores, :checksum, :unique => true, :name => 'uniq_semaphore_checksums'
    add_varchar_index :semaphores, :name, :name => 'semaphore_names'

  end

end

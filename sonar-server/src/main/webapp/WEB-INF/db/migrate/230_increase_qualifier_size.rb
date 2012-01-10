#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
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

#
# Sonar 2.13
#
class IncreaseQualifierSize < ActiveRecord::Migration

  def self.up
    dialect = ActiveRecord::Base.configurations[ ENV['RAILS_ENV'] ]["dialect"]

    if dialect == 'sqlserver'
      remove_index :snapshots, :name => 'snapshots_qualifier'
    end

    change_column('snapshots', 'qualifier', :string, :limit => 10, :null => true)
    change_column('projects', 'qualifier', :string, :limit => 10, :null => true)

    if dialect == 'sqlserver'
      add_index :snapshots, :qualifier, :name => 'snapshots_qualifier'
    end
  end

end

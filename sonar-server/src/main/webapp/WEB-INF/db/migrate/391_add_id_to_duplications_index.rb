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

#
# Sonar 3.6
# SONAR-3340
class AddIdToDuplicationsIndex < ActiveRecord::Migration

  class ProjectMeasure < ActiveRecord::Base
  end

  def self.up
    case dialect()
    when "oracle"
      upgrade_oracle()
    else
      add_column :duplications_index, :id, :primary_key
    end
  end

  private

  def self.upgrade_oracle
    execute_ddl('create sequence', 'CREATE SEQUENCE duplications_index_seq START WITH 1 INCREMENT BY 1 NOMAXVALUE')
    execute_ddl('add id column', 'ALTER TABLE duplications_index add "ID" NUMBER(38)')
    execute_ddl('create id for existing data','UPDATE duplications_index SET "ID" = duplications_index_seq.nextval')
    execute_ddl('add primary key constraint', 'ALTER TABLE duplications_index ADD CONSTRAINT pk_duplications_index PRIMARY KEY( "ID" )')
  end

  def self.execute_ddl(message, ddl)
    begin
      say_with_time(message) do
        ProjectMeasure.connection.execute(ddl)
      end
    rescue
      # already executed
    end
  end

end


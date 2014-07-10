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
# Sonar 3.6
# SONAR-3340
class AddIdToDuplicationsIndex < ActiveRecord::Migration


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
    #Use CTAS with NOLOGGING for better performances and to avoid issues with REDO/UNDO logs
    execute_ddl('create sequence', 'CREATE SEQUENCE duplications_index_seq START WITH 1 INCREMENT BY 1 NOMAXVALUE')
    execute_ddl('create new table with id', 'CREATE TABLE new_duplications_index ("ID" PRIMARY KEY, project_snapshot_id, snapshot_id, hash, index_in_file, start_line, end_line) NOLOGGING AS '\
                                 'SELECT CAST(duplications_index_seq.nextval AS NUMBER(38)) "ID", project_snapshot_id, snapshot_id, hash, index_in_file, start_line, end_line FROM duplications_index')
    execute_ddl('drop old table','DROP TABLE duplications_index CASCADE CONSTRAINTS PURGE')
    execute_ddl('rename new table', 'RENAME new_duplications_index TO duplications_index')
    
    add_index :duplications_index, :project_snapshot_id, :name => 'duplications_index_psid'
    add_index :duplications_index, :snapshot_id, :name => 'duplications_index_sid'
    add_index :duplications_index, :hash, :name => 'duplications_index_hash'

    execute_ddl('enable logging on new table', 'ALTER TABLE duplications_index LOGGING')
  end

  def self.execute_ddl(message, ddl)
    begin
      say_with_time(message) do
        ActiveRecord::Base.connection.execute(ddl)
      end
    rescue
      # already executed
    end
  end

end


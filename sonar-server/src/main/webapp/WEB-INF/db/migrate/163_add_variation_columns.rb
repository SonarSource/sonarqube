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

#
# Sonar 2.5
#
class AddVariationColumns < ActiveRecord::Migration

  def self.up
    dialect = ActiveRecord::Base.configurations[ ENV['RAILS_ENV'] ]["dialect"]
    say "Detected dialect: #{dialect}"

    case dialect
    when "sqlserver"
      upgrade_sqlserver()
    when "oracle"
      upgrade_oracle()
    when "mysql"
      upgrade_mysql()
    when "postgre"
      upgrade_postgresql()
    else
      remove_measures_column(:diff_value_1)
      remove_measures_column(:diff_value_2)
      remove_measures_column(:diff_value_3)

      add_measures_column('variation_value_1')
      add_measures_column('variation_value_2')
      add_measures_column('variation_value_3')
      add_measures_column('variation_value_4')
      add_measures_column('variation_value_5')    
    end
  end

  private
  
  def self.upgrade_sqlserver
    execute_ddl('drop measures columns', 'ALTER TABLE project_measures DROP COLUMN diff_value_1, diff_value_2, diff_value_3')
    execute_ddl('add measures columns', 'ALTER TABLE project_measures ADD variation_value_1 DECIMAL(30, 20) NULL DEFAULT NULL, variation_value_2 DECIMAL(30, 20) NULL DEFAULT NULL, variation_value_3 DECIMAL(30, 20) NULL DEFAULT NULL, variation_value_4 DECIMAL(30, 20) NULL DEFAULT NULL, variation_value_5 DECIMAL(30, 20) NULL DEFAULT NULL')
    execute_ddl('add snapshots columns','ALTER TABLE snapshots ADD 
          period1_mode VARCHAR(100) DEFAULT NULL NULL,
          period1_param VARCHAR(100) DEFAULT NULL NULL,
          period1_date DATETIME DEFAULT NULL NULL,

          period2_mode VARCHAR(100) DEFAULT NULL NULL,
          period2_param VARCHAR(100) DEFAULT NULL NULL,
          period2_date DATETIME DEFAULT NULL NULL,

          period3_mode VARCHAR(100) DEFAULT NULL NULL,
          period3_param VARCHAR(100) DEFAULT NULL NULL,
          period3_date DATETIME DEFAULT NULL NULL,

          period4_mode VARCHAR(100) DEFAULT NULL NULL,
          period4_param VARCHAR(100) DEFAULT NULL NULL,
          period4_date DATETIME DEFAULT NULL NULL,

          period5_mode VARCHAR(100) DEFAULT NULL NULL,
          period5_param VARCHAR(100) DEFAULT NULL NULL,
          period5_date DATETIME DEFAULT NULL NULL
      ')
  end
  
  def self.upgrade_oracle
    execute_ddl('drop measures columns', 'ALTER TABLE project_measures DROP (diff_value_1, diff_value_2, diff_value_3)')
    
    execute_ddl('add measures columns','ALTER TABLE project_measures 
    	ADD (
    		variation_value_1 NUMBER(30, 20),
    		variation_value_2 NUMBER(30, 20),
    		variation_value_3 NUMBER(30, 20),
    		variation_value_4 NUMBER(30, 20),
    		variation_value_5 NUMBER(30, 20))')
    		
    execute_ddl('add snapshots columns', 'ALTER TABLE snapshots 
        	ADD (
        		period1_mode VARCHAR2(100),
        		period1_param VARCHAR2(100),
        		period1_date TIMESTAMP(6),

        		period2_mode VARCHAR2(100),
        		period2_param VARCHAR2(100),
        		period2_date TIMESTAMP(6),

        		period3_mode VARCHAR2(100),
        		period3_param VARCHAR2(100),
        		period3_date TIMESTAMP(6),

        		period4_mode VARCHAR2(100),
        		period4_param VARCHAR2(100),
        		period4_date TIMESTAMP(6),

        		period5_mode VARCHAR2(100),
        		period5_param VARCHAR2(100),
        		period5_date TIMESTAMP(6))')
  end
  
  def self.upgrade_mysql
    execute_ddl('alter measures table','ALTER TABLE project_measures 
    	DROP COLUMN diff_value_1,
    	DROP COLUMN diff_value_2,
    	DROP COLUMN diff_value_3,
    	ADD COLUMN variation_value_1 DECIMAL(30, 20) NULL DEFAULT NULL,
    	ADD COLUMN variation_value_2 DECIMAL(30, 20) NULL DEFAULT NULL,
    	ADD COLUMN variation_value_3 DECIMAL(30, 20) NULL DEFAULT NULL,
    	ADD COLUMN variation_value_4 DECIMAL(30, 20) NULL DEFAULT NULL,
    	ADD COLUMN variation_value_5 DECIMAL(30, 20) NULL DEFAULT NULL')
    	
    execute_ddl('add snapshots columns','ALTER TABLE snapshots 
    	ADD COLUMN period1_mode VARCHAR(100) NULL DEFAULT NULL,
    	ADD COLUMN period1_param VARCHAR(100) NULL DEFAULT NULL,
    	ADD COLUMN period1_date DATETIME NULL DEFAULT NULL,

    	ADD COLUMN period2_mode VARCHAR(100) NULL DEFAULT NULL,
    	ADD COLUMN period2_param VARCHAR(100) NULL DEFAULT NULL,
    	ADD COLUMN period2_date DATETIME NULL DEFAULT NULL,

    	ADD COLUMN period3_mode VARCHAR(100) NULL DEFAULT NULL,
    	ADD COLUMN period3_param VARCHAR(100) NULL DEFAULT NULL,
    	ADD COLUMN period3_date DATETIME NULL DEFAULT NULL,

    	ADD COLUMN period4_mode VARCHAR(100) NULL DEFAULT NULL,
    	ADD COLUMN period4_param VARCHAR(100) NULL DEFAULT NULL,
    	ADD COLUMN period4_date DATETIME NULL DEFAULT NULL,

    	ADD COLUMN period5_mode VARCHAR(100) NULL DEFAULT NULL,
    	ADD COLUMN period5_param VARCHAR(100) NULL DEFAULT NULL,
    	ADD COLUMN period5_date DATETIME NULL DEFAULT NULL')
  end
  
  def self.upgrade_postgresql
    execute_ddl('alter measures table','ALTER TABLE project_measures 
    	DROP COLUMN diff_value_1,
    	DROP COLUMN diff_value_2,
    	DROP COLUMN diff_value_3,	
    	ADD variation_value_1 numeric,
    	ADD variation_value_2 numeric,
    	ADD variation_value_3 numeric,
    	ADD variation_value_4 numeric,
    	ADD variation_value_5 numeric')
    	
    execute_ddl('add snapshots columns','ALTER TABLE snapshots 
        ADD period1_mode character varying(100),
        ADD period1_param character varying(100),
        ADD period1_date timestamp without time zone,

        ADD period2_mode character varying(100),
        ADD period2_param character varying(100),
        ADD period2_date timestamp without time zone,

        ADD period3_mode character varying(100),
        ADD period3_param character varying(100),
        ADD period3_date timestamp without time zone,

        ADD period4_mode character varying(100),
        ADD period4_param character varying(100),
        ADD period4_date timestamp without time zone,

        ADD period5_mode character varying(100),
        ADD period5_param character varying(100),
        ADD period5_date timestamp without time zone')
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
  
  def self.remove_measures_column(colname)
    begin
      remove_column :project_measures, colname
      ProjectMeasure.reset_column_information()
    rescue
      # already removed
    end
  end

  def self.add_measures_column(colname)
    unless ProjectMeasure.column_names.include?(name)
      add_column(:project_measures, colname, :decimal, :null => true, :precision => 30, :scale => 20)
      ProjectMeasure.reset_column_information()
    end
  end
end

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
class InitialSchema < ActiveRecord::Migration
  def self.up
    create_table :projects do |t|
      t.column :name,                :string,    :null => true,  :limit => 256
      t.column :description,         :string,    :null => true,  :limit => 2000
      t.column :enabled,             :boolean,   :null => false, :default => true
      t.column 'scope', :string, :limit => 3
      t.column 'qualifier', :string, :limit => 10, :null => true
      t.column 'kee', :string, :limit => 400
      t.column 'root_id', :integer
      t.column :language, :string, :null => true, :limit => 20
      t.column :copy_resource_id, :integer, :null => true
      t.column :long_name, :string, :null => true, :limit => 256
      t.column :person_id, :integer, :null => true
      t.column :created_at, :datetime, :null => true
    end

    create_table :snapshots do |t|
      t.column :created_at,          :datetime,  :null => true, :default => nil
      t.column :project_id,          :integer,   :null => false
      t.column :parent_snapshot_id, :integer,   :null => true
      t.column :status,             :string,    :null => false, :default => 'U', :limit => 4
      t.column :islast,             :boolean,   :null => false, :default => false
      t.column 'scope', :string, :limit => 3
      t.column 'qualifier', :string, :limit => 10
      t.column 'root_snapshot_id', :integer
      t.column 'version', :string, :limit => 60, :null => true
      t.column :path, :string, :null => true, :limit => 96
      t.column :depth, :integer, :null => true
      t.column :root_project_id, :integer, :nullable => true
      t.column 'build_date', :datetime, :null => true
      t.column 'purge_status', :integer, :null => true
    end

    create_table :metrics do |t|
      t.column :name,                :string,    :null => false, :limit => 64
      t.column :description,         :string,    :null => true, :limit => 255
      t.column :direction,           :integer,   :null => false, :default => 0
      t.column :domain, :string, :null => true, :limit => 64
      t.column :short_name, :string, :null => true, :limit => 64
      t.column :qualitative, :boolean, :null => false, :default => false
      t.column :val_type, :string, :null => true, :limit => 8
      t.column :user_managed, :boolean, :null => true, :default => false
      t.column :enabled, :boolean, :null => true, :default => true
      t.column :origin, :string, :null => true, :limit => 3
      t.column 'worst_value', :decimal, :null => true, :precision => 30, :scale => 20
      t.column 'best_value', :decimal, :null => true, :precision => 30, :scale => 20
      t.column 'optimized_best_value', :boolean, :null => true
      t.column 'hidden', :boolean, :null => true
      t.column 'delete_historical_data', :boolean, :null => true
    end

    create_table :project_measures do |t|
      t.column :value,               :decimal,   :null => true, :precision => 30, :scale => 20
      t.column :metric_id,           :integer,   :null => false
      t.column :snapshot_id,         :integer,   :null => true
      t.column :rule_id,             :integer
      t.column :rules_category_id,   :integer
      t.column :text_value, :string, :limit => 96, :null => true
      t.column :tendency, :integer, :null => true
      t.column :measure_date, :datetime, :null => true
      t.column :project_id, :integer, :null => true
      t.column :alert_status, :string, :limit => 5, :null => true
      t.column :alert_text, :string, :null => true, :limit => 4000
      t.column :url, :string, :null => true, :limit => 2000
      t.column :description, :string, :null => true, :limit => 4000
      t.column :rule_priority, :integer, :null => true
      t.column :diff_value_1, :decimal, :null => true, :precision => 30, :scale => 20
      t.column :diff_value_2, :decimal, :null => true, :precision => 30, :scale => 20
      t.column :diff_value_3, :decimal, :null => true, :precision => 30, :scale => 20
      t.column :characteristic_id, :integer, :null => true
      t.column 'person_id', :integer, :null => true
    end

    create_table :rules do |t|
      t.column :name,                :string,    :null => true, :limit => 200
      t.column :plugin_rule_key,     :string,    :null => false, :limit => 200
      t.column :plugin_config_key,   :string,    :null => true, :limit => 200
      t.column :plugin_name,         :string,    :null => false, :limit => 255
      t.column :description,         :text,      :null => true
      t.column :priority, :integer, :null => true
      t.column :cardinality, :string, :null => true, :limit => 10
      t.column :parent_id, :integer, :null => true
      t.column 'status', :string, :null => true, :limit => 40
      t.column 'language', :string, :null => true, :limit => 20
      t.column 'created_at', :datetime, :null => true
      t.column 'updated_at', :datetime, :null => true
    end

    create_table :rules_parameters do |t|
      t.column :rule_id,             :integer,   :null => false
      t.column :name,                :string,    :null => false, :limit => 128
      t.column :description,         :string,    :null => true, :limit => 4000
      t.column :param_type,          :string,    :null => false, :limit => 512
      t.column :default_value,       :string, :null => true, :limit => 4000
    end

    create_table :project_links do |t|
      t.column :project_id,          :integer,   :null => false
      t.column :link_type,           :string,    :null => true,  :limit => 20
      t.column :name,                :string,    :null => true,  :limit => 128
      t.column :href,                :string,    :null => false, :limit => 2048
    end
  end
end

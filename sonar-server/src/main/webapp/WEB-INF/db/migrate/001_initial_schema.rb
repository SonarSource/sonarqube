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
class InitialSchema < ActiveRecord::Migration
  def self.up
    create_table :projects do |t|
      t.column :group_id,            :string,    :null => false, :limit => 256
      t.column :artifact_id,         :string,    :null => false, :limit => 256
      t.column :branch,              :string,    :null => false, :limit => 64
      t.column :name,                :string,    :null => true,  :limit => 256
      t.column :description,         :string,    :null => true,  :limit => 2000
      t.column :enabled,             :boolean,   :null => false, :default => true
    end

    create_table :snapshots do |t|
      t.column :created_at,          :datetime,  :null => true, :default => nil
      t.column :version,             :string,    :null => false, :limit => 32
      t.column :project_id,          :integer,   :null => false
      t.column :parent_snapshot_id, :integer,   :null => true
      t.column :status,             :string,    :null => false, :default => 'U', :limit => 4
      t.column :purged,             :boolean,   :null => false, :default => false
      t.column :islast,             :boolean,   :null => false, :default => false

    end

    create_table :metrics do |t|
      t.column :name,                :string,    :null => false, :limit => 64
      t.column :value_type,          :integer,   :null => false
      t.column :description,         :string,    :null => true, :limit => 255
      t.column :direction,           :integer,   :null => false, :default => 0
    end

    create_table :project_measures do |t|
      t.column :value,               :decimal,   :null => true, :precision => 30, :scale => 20
      t.column :metric_id,           :integer,   :null => false
      t.column :snapshot_id,         :integer,   :null => true
      t.column :rule_id,             :integer
      t.column :rules_category_id,   :integer
    end

    create_table :files do |t|
      t.column :snapshot_id,         :integer,   :null => false
      t.column :filename,            :string,    :limit => 255
      t.column :namespace,           :string,    :limit => 500
      t.column :path,                :string,    :limit => 500
    end

    create_table :rules_categories do |t|
      t.column :name,                :string,    :null => false, :limit => 255
      t.column :description,         :string,    :null => false, :limit => 1000
    end

    create_table :rules do |t|
      t.column :name,                :string,    :null => false, :limit => 128
      t.column :rules_category_id,   :integer,   :null => false
      t.column :plugin_rule_key,     :string,    :null => false, :limit => 200
      t.column :plugin_config_key,   :string,    :null => false, :limit => 200
      t.column :plugin_name,         :string,    :null => false, :limit => 255
      t.column :description,         :text
    end

    create_table :parameters do |t|
      t.column :param_key,           :string,    :null => false, :limit => 100, :null => false
      t.column :value,               :decimal,   :null => false, :precision => 30, :scale => 20
      t.column :value2,              :decimal,   :null => true, :precision => 30, :scale => 20
    end

    create_table :rule_failures do |t|
      t.column :snapshot_id,         :integer,   :null => false
      t.column :rule_id,             :integer,   :null => false
      t.column :failure_level,       :integer,   :null => false
      t.column :file_id,             :integer
      t.column :message,             :string,    :limit => 500
    end

    create_table :rules_parameters do |t|
      t.column :rule_id,             :integer,   :null => false
      t.column :name,                :string,    :null => false, :limit => 128
      t.column :description,         :string,    :null => false, :limit => 4000
      t.column :param_type,          :string,    :null => false, :limit => 512
      t.column :default_value,       :string,    :null => true,  :limit => 4000
    end

    create_table :project_links do |t|
      t.column :project_id,          :integer,   :null => false
      t.column :link_type,           :string,    :null => true,  :limit => 20
      t.column :name,                :string,    :null => true,  :limit => 128
      t.column :href,                :string,    :null => false, :limit => 2048
    end

  end

  def self.down
    drop_table :project_links
    drop_table :rules_parameters
    drop_table :rule_failures
    drop_table :parameters
    drop_table :rules
    drop_table :rules_categories
    drop_table :files
    drop_table :project_measures
    drop_table :metrics
    drop_table :snapshots
    drop_table :projects
  end
end

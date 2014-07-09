class AddRulesDescriptionFormat < ActiveRecord::Migration

  class Rule < ActiveRecord::Base
  end

  def self.up
    add_column :rules, :description_format, :string, :null => true, :limit => 20

    Rule.reset_column_information
    Rule.update_all({:description_format => 'HTML', :updated_at => Time.now})
  end

end

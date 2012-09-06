# Used just for Rails 2.x
# In Rails 3.x rake tasks are loaded using railtie
if ActiveRecord::VERSION::MAJOR == 2

  if defined?(Rake.application) && Rake.application &&
      ActiveRecord::Base.configurations[defined?(Rails.env) ? Rails.env : RAILS_ENV]['adapter'] == 'oracle_enhanced'
    oracle_enhanced_rakefile = File.dirname(__FILE__) + "/oracle_enhanced.rake"
    if Rake.application.lookup("environment")
      # rails tasks already defined; load the override tasks now
      load oracle_enhanced_rakefile
    else
      # rails tasks not loaded yet; load as an import
      Rake.application.add_import(oracle_enhanced_rakefile)
    end
  end

end

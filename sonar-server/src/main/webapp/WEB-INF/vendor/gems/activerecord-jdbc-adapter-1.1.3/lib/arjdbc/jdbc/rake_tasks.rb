if defined?(Rake.application) && Rake.application && ENV["SKIP_AR_JDBC_RAKE_REDEFINES"].nil?
  jdbc_rakefile = File.dirname(__FILE__) + "/jdbc.rake"
  if Rake.application.lookup("db:create")
    # rails tasks already defined; load the override tasks now
    load jdbc_rakefile
  else
    # rails tasks not loaded yet; load as an import
    Rake.application.add_import(jdbc_rakefile)
  end
end

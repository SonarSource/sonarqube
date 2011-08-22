if RUBY_PLATFORM =~ /java/
  begin
    tried_gem ||= false
    require 'active_record/version'
  rescue LoadError
    raise if tried_gem
    require 'rubygems'
    gem 'activerecord'
    tried_gem = true
    retry
  end
  if ActiveRecord::VERSION::MAJOR < 2
    if defined?(RAILS_CONNECTION_ADAPTERS)
      RAILS_CONNECTION_ADAPTERS << %q(jdbc)
    else
      RAILS_CONNECTION_ADAPTERS = %w(jdbc)
    end
    if ActiveRecord::VERSION::MAJOR == 1 && ActiveRecord::VERSION::MINOR == 14
      require 'active_record/connection_adapters/jdbc_adapter'
    end
  else
    require 'active_record'
    require 'active_record/connection_adapters/jdbc_adapter'
  end
else
  warn "ActiveRecord-JDBC is for use with JRuby only"
end

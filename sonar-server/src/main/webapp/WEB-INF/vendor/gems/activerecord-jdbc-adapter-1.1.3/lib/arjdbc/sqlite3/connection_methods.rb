# Don't need to load native sqlite3 adapter
$LOADED_FEATURES << "active_record/connection_adapters/sqlite_adapter.rb"
$LOADED_FEATURES << "active_record/connection_adapters/sqlite3_adapter.rb"

class ActiveRecord::Base
  class << self
    def sqlite3_connection(config)
      require "arjdbc/sqlite3"

      parse_sqlite3_config!(config)
      database = config[:database]
      database = '' if database == ':memory:'
      config[:url] ||= "jdbc:sqlite:#{database}"
      config[:driver] ||= "org.sqlite.JDBC"
      config[:adapter_class] = ActiveRecord::ConnectionAdapters::SQLite3Adapter
      jdbc_connection(config)
    end

    def parse_sqlite3_config!(config)
      config[:database] ||= config[:dbfile]

      # Allow database path relative to RAILS_ROOT, but only if
      # the database path is not the special path that tells
      # Sqlite to build a database only in memory.
      rails_root_defined = defined?(Rails.root) || Object.const_defined?(:RAILS_ROOT)
      if rails_root_defined && ':memory:' != config[:database]
        rails_root = defined?(Rails.root) ? Rails.root : RAILS_ROOT
        config[:database] = File.expand_path(config[:database], rails_root)
      end
    end

    alias_method :jdbcsqlite3_connection, :sqlite3_connection
  end
end

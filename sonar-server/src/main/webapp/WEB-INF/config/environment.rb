# Be sure to restart your server when you modify this file

# Uncomment below to force Rails into production mode when
# you don't control web/app server and can't set it the proper way
# ENV['RAILS_ENV'] ||= 'production'

# Specifies gem version of Rails to use when vendor/rails is not present
RAILS_GEM_VERSION = '2.2.2' unless defined? RAILS_GEM_VERSION

# Bootstrap the Rails environment, frameworks, and default configuration
require File.join(File.dirname(__FILE__), 'boot')
require 'color'

Rails::Initializer.run do |config|
  # Settings in config/environments/* take precedence over those specified here.
  # Application configuration should go into files in config/initializers
  # -- all .rb files in that directory are automatically loaded.
  # See Rails::Configuration for more options.

  # Skip frameworks you're not going to use. To use Rails without a database
  # you must remove the Active Record framework.
  config.frameworks -= [ :action_mailer ]

  # Specify gems that this application depends on. 
  # They can then be installed with "rake gems:install" on new installations.
  # You have to specify the :lib option for libraries, where the Gem name (sqlite3-ruby) differs from the file itself (sqlite3)
  # config.gem "bj"
  # config.gem "hpricot", :version => '0.6', :source => "http://code.whytheluckystiff.net"
  # config.gem "sqlite3-ruby", :lib => "sqlite3"
  # config.gem "aws-s3", :lib => "aws/s3"

  # Only load the plugins named here, in the order given. By default, all plugins 
  # in vendor/plugins are loaded in alphabetical order.
  # :all can be used as a placeholder for all plugins not explicitly named
  # config.plugins = [ :exception_notification, :ssl_requirement, :all ]

  # Add additional load paths for your own custom dirs
  # config.load_paths += %W( #{RAILS_ROOT}/extras )

  # Force all environments to use the same logger level
  # (by default production uses :info, the others :debug)
  # config.log_level = :debug

  # Make Time.zone default to the specified zone, and make Active Record store time values
  # in the database in UTC, and return them converted to the specified local zone.
  # Run "rake -D time" for a list of tasks for finding time zone names. Comment line to use default local time.
  # config.time_zone = 'UTC'

  # The internationalization framework can be changed to have another default locale (standard is :en) or more load paths.
  # All files from config/locales/*.rb,yml are added automatically.
  # config.i18n.load_path << Dir[File.join(RAILS_ROOT, 'my', 'locales', '*.{rb,yml}')]
  # config.i18n.default_locale = :de
  config.i18n.load_path += Dir[File.join(RAILS_ROOT, 'config', 'locales', '**', '*.{rb,yml}')] 
  config.i18n.default_locale = :en

  # Your secret key for verifying cookie session data integrity.
  # If you change this key, all old sessions will become invalid!
  # Make sure the secret is at least 30 characters and all random, 
  # no regular words or you'll be exposed to dictionary attacks.
  config.action_controller.session = {
    :session_key => '_sonar_session',
    :secret      => '095e565aa414bca3f8f21c2501189b92ec9c5220d62d20f36ffc1c676351c42a6ce81d2685e1788b5bf4b4c3832eb8a70497581c6106815483df65b125f7197d'
  }

  # Use the database for sessions instead of the cookie-based default,
  # which shouldn't be used to store highly confidential information
  # (create the session table with "rake db:sessions:create")
  # config.action_controller.session_store = :active_record_store

  # Use SQL instead of Active Record's schema dumper when creating the test database.
  # This is necessary if your schema can't be completely dumped by the schema dumper,
  # like if you have constraints or database-specific column types
  # config.active_record.schema_format = :sql

  # Activate observers that should always be running
  # Please note that observers generated using script/generate observer need to have an _observer suffix
  # config.active_record.observers = :cacher, :garbage_collector, :forum_observer
end

class ActiveRecord::Migration

  def self.alter_to_big_primary_key(tablename)
    dialect = ActiveRecord::Base.configurations[ ENV['RAILS_ENV'] ]["dialect"]
    case dialect
    when "postgre"
      execute "ALTER TABLE #{tablename} ALTER COLUMN id TYPE bigint"
    when "mysql"
      execute "ALTER TABLE #{tablename} CHANGE id id BIGINT AUTO_INCREMENT";
    when "derby"
      # do nothing as alter can not do the job in Derby
    when "oracle"
      # do nothing, oracle integer are big enough
    when "sqlserver"
        constraint=select_one "select name from sysobjects where parent_obj = (select id from sysobjects where name = '#{tablename}')"
        execute "ALTER TABLE #{tablename} DROP CONSTRAINT #{constraint["name"]}"
  	    execute "ALTER TABLE #{tablename} ALTER COLUMN id bigint"
    	  execute "ALTER TABLE #{tablename} ADD PRIMARY KEY(id)"
    end
  end

  def self.alter_to_big_integer(tablename, columnname, indexname=nil)
    dialect = ActiveRecord::Base.configurations[ ENV['RAILS_ENV'] ]["dialect"]
    case dialect
     when "sqlserver"
     		execute "DROP INDEX #{indexname} on #{tablename}" if indexname
     		change_column(tablename, columnname, :big_integer, :null => true)
     		execute "CREATE INDEX #{indexname} on #{tablename}(#{columnname})" if indexname
     else
		   change_column(tablename, columnname, :big_integer, :null => true)
     end
  end
end

module JdbcSpec

	#
	# Ticket http://tools.assembla.com/sonar/ticket/200
	# Problem with mysql TEXT columns. ActiveRecord :text type is mapped to TEXT type (65535 characters). 
	# But we would like the bigger MEDIUMTEXT for the snapshot_sources table (16777215  characters).
	# This hack works only for ActiveRecord-JDBC (Jruby use).
	# See http://www.headius.com/jrubywiki/index.php/Adding_Datatypes_to_ActiveRecord-JDBC
	# The following has been copied from WEB-INF\gems\gems\activerecord-jdbc-adapter-0.9\lib\jdbc_adapter\jdbc_mysql.rb
  # Problem still in activerecord-jdbc-adapter 0.9
  module MySQL
    def modify_types(tp)
      tp[:primary_key] = "int(11) DEFAULT NULL auto_increment PRIMARY KEY"
      tp[:decimal] = { :name => "decimal" }
      tp[:timestamp] = { :name => "datetime" }
      tp[:datetime][:limit] = nil
      
      # sonar
      tp[:text] = { :name => "mediumtext" }
      tp[:binary] = { :name => "longblob" }
      tp[:big_integer] = { :name => "bigint"}

      tp
    end
  end
  
  # wrong column types on oracle 10g timestamp and datetimes
  # Problem still in activerecord-jdbc-adapter 0.8
  module Oracle
    def modify_types(tp)
      tp[:primary_key] = "NUMBER(38) NOT NULL PRIMARY KEY"
      tp[:integer] = { :name => "NUMBER", :limit => 38 }
      tp[:datetime] = { :name => "TIMESTAMP" }  # updated for sonar
      tp[:timestamp] = { :name => "TIMESTAMP" } # updated for sonar
      tp[:time] = { :name => "DATE" }
      tp[:date] = { :name => "DATE" }

      #sonar
      tp[:big_integer] = { :name => "NUMBER", :limit => 38 }

      tp
    end

  end

  module MsSQL
    def modify_types(tp)
      tp[:primary_key] = "int NOT NULL IDENTITY(1, 1) PRIMARY KEY"
      tp[:integer][:limit] = nil
      tp[:boolean] = {:name => "bit"}
      tp[:binary] = { :name => "image"}

      # sonar patch:
      tp[:text] = { :name => "NVARCHAR(MAX)" }
      tp[:big_integer] = { :name => "bigint"}
    end

  end
  
  # activerecord-jdbc-adapter has a missing quote_table_name method
  module Derby
    def modify_types(tp)
      tp[:primary_key] = "int generated by default as identity NOT NULL PRIMARY KEY"
      tp[:integer][:limit] = nil
      tp[:string][:limit] = 256
      tp[:boolean] = {:name => "smallint"}

      #sonar
      tp[:big_integer] = {:name => "bigint"}

      tp
    end

    def quote_table_name(name) #:nodoc:
      quote_column_name(name).gsub('.', '`.`')
    end
  end

  module PostgreSQL
    def modify_types(tp)
      tp[:primary_key] = "serial primary key"
      tp[:integer][:limit] = nil
      tp[:boolean][:limit] = nil

      # sonar
      # tp[:string][:limit] = 255
      tp[:big_integer] = { :name => "int8", :limit => nil }

      tp
    end

    # See SONAR-862 on Postgre search_path setting.
    # The issue is fixed in next activerecord-jdbc-adapter version: http://github.com/nicksieger/activerecord-jdbc-adapter/commit/2575700d3aee2eb395cac3e7933bb4d129fa2f03 
    # More details on https://rails.lighthouseapp.com/projects/8994/tickets/918-postgresql-tables-not-generating-correct-schema-list
    def columns(table_name, name=nil)
      # schema_name must be nil instead of "public"
      schema_name = nil
      if table_name =~ /\./
        parts = table_name.split(/\./)
        table_name = parts.pop
        schema_name = parts.join(".")
      end
      @connection.columns_internal(table_name, name, schema_name)
    end
  end
end

module ActionView
  module Helpers
    module NumberHelper

      def number_with_precision(number, *args)
        options = args.extract_options!
        options.symbolize_keys!

        defaults           = I18n.translate('number.format', :locale => options[:locale], :raise => true) rescue {}

        # SONAR : do not merge with 'number.precision.format'. It usually removes definitions of delimiter and separator.
        # It looks to be fixed in Rails 2.3.
        #precision_defaults = I18n.translate('number.precision.format''number.precision.format', :locale => options[:locale],
        #defaults           = defaults.merge(precision_defaults)

        unless args.empty?
          ActiveSupport::Deprecation.warn('number_with_precision takes an option hash ' +
            'instead of a separate precision argument.', caller)
          precision = args[0] || defaults[:precision]
        end

        precision ||= (options[:precision] || defaults[:precision])
        separator ||= (options[:separator] || defaults[:separator])
        delimiter ||= (options[:delimiter] || defaults[:delimiter])

        begin
          rounded_number = (Float(number) * (10 ** precision)).round.to_f / 10 ** precision
          number_with_delimiter("%01.#{precision}f" % rounded_number,
            :separator => separator,
            :delimiter => delimiter)
        rescue
          number
        end
      end
    end
  end
end

# patch for SONAR-1182. GWT does not support ISO8601 dates that end with 'Z'
# http://google-web-toolkit.googlecode.com/svn/javadoc/1.6/com/google/gwt/i18n/client/DateTimeFormat.html
module ActiveSupport
  class TimeWithZone
    def xmlschema
      # initial code: "#{time.strftime("%Y-%m-%dT%H:%M:%S")}#{formatted_offset(true, 'Z')}"
      "#{time.strftime("%Y-%m-%dT%H:%M:%S")}#{formatted_offset(true, nil)}"
    end
  end
end

# other patches :
# - activerecord 2.2.2: fix Oracle bug when more than 1000 elements in IN clause. See lib/active_record/association_preload.rb
# See https://rails.lighthouseapp.com/projects/8994/tickets/1533-preloading-more-than-1000-associated-records-causes-activerecordstatementinvalid-when-using-oracle

require File.dirname(__FILE__) + '/../lib/sonar_webservice_plugins.rb'
require File.dirname(__FILE__) + '/../lib/database_version.rb'
DatabaseVersion.automatic_setup

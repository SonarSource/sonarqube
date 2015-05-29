RAILS_GEM_VERSION = '2.3.15'

# Avoid conflict with local ruby installations
# See http://jira.sonarsource.com/browse/SONAR-3579
ENV['GEM_HOME'] = $servlet_context.getRealPath('/WEB-INF/gems')

# Bootstrap the Rails environment, frameworks, and default configuration
require File.join(File.dirname(__FILE__), 'boot')
require 'color'

# Disable all the warnings :
# Gem::SourceIndex#initialize called from /.../web/WEB-INF/gems/gems/rails-2.3.15/lib/rails/vendor_gem_source_index.rb:100.
# The other solutions are to upgrade to rails 3 or to use gembundler.com
require 'rubygems'
Gem::Deprecate.skip = (RAILS_ENV == 'production')

# Needed to support rails 2.3 with latest gem provided by JRuby
# See http://djellemah.com/blog/2013/02/27/rails-23-with-ruby-20/
module Gem
  def self.source_index
    sources
  end

  def self.cache
    sources
  end

  SourceIndex = Specification

  class SourceList
    # If you want vendor gems, this is where to start writing code.
    def search( *args ); []; end
    def each( &block ); end
    include Enumerable
  end
end



#
# Limitation of Rails 2.3 and Rails Engines (plugins) when threadsafe! is enabled in production mode
# See http://groups.google.com/group/rubyonrails-core/browse_thread/thread/9067bce01444fb24?pli=1
#
class EagerPluginLoader < Rails::Plugin::Loader
  def add_plugin_load_paths
    super
    plugins.each do |plugin|
      if configuration.cache_classes
        configuration.eager_load_paths += plugin.load_paths
      end
    end
  end
end


#
# Put response headers on all HTTP calls. This is done by the Java SecurityServlerFilter,
# but for some reason Rack swallows the headers set on Java side.
# See middleware configuration below.
#
class SecurityHeaders
  def initialize(app)
    @app = app
  end

  def call(env)
    status, headers, body = @app.call(env)

    # Clickjacking protection
    # See https://www.owasp.org/index.php/Clickjacking_Protection_for_Java_EE
    headers['X-Frame-Options']='SAMEORIGIN'

    # Cross-site scripting
    # See https://www.owasp.org/index.php/List_of_useful_HTTP_headers
    headers['X-XSS-Protection']='1; mode=block'

    # MIME-sniffing
    # See https://www.owasp.org/index.php/List_of_useful_HTTP_headers
    headers['X-Content-Type-Options']='nosniff';

    [status, headers, body]
  end
end


Rails::Initializer.run do |config|
  # Settings in config/environments/* take precedence over those specified here.
  # Application configuration should go into files in config/initializers
  # -- all .rb files in that directory are automatically loaded.
  # See Rails::Configuration for more options.

  # Skip frameworks you're not going to use. To use Rails without a database
  # you must remove the Active Record framework.
  config.frameworks -= [:action_mailer, :active_resource]

  # This property can't be set in config/environments because of execution order
  # See http://strd6.com/2009/04/cant-dup-nilclass-maybe-try-unloadable/
  config.reload_plugins=(RAILS_ENV == 'development')

  config.plugin_loader = EagerPluginLoader

  # Load the applications that are packaged with sonar plugins.
  # The development mode (real-time edition of ruby code) can be enabled on an app by replacing the
  # following line by :
  # config.plugin_paths << '/absolute/path/to/myproject/src/main/resources/org/sonar/ror'
  config.plugin_paths << "#{Java::JavaLang::System.getProperty("java.io.tmpdir")}/ror"

  # Force all environments to use the same logger level
  # (by default production uses :info, the others :debug)
  # config.log_level = :debug

  # Make Time.zone default to the specified zone, and make Active Record store time values
  # in the database in UTC, and return them converted to the specified local zone.
  # Run "rake -D time" for a list of tasks for finding time zone names. Comment line to use default local time.
  # config.time_zone = 'UTC'

  # The internationalization framework can be changed to have another default locale (standard is :en) or more load paths.
  # All files from config/locales/*.rb,yml are added automatically.

  # Default locales provided by Ruby on Rails
  config.i18n.load_path << Dir[File.join(RAILS_ROOT, 'config', 'locales', '**', '*.{rb,yml}')]

  # Overridden bundles
  config.i18n.load_path << Dir[File.join(RAILS_ROOT, 'config', 'locales', '*.{rb,yml}')]

  config.i18n.default_locale = :en

  # Provided by JRuby-Rack
  config.action_controller.session_store = :java_servlet_store

  # Prevent appearance of ANSI style escape sequences in logs
  config.active_record.colorize_logging = false

  # Use SQL instead of Active Record's schema dumper when creating the test database.
  # This is necessary if your schema can't be completely dumped by the schema dumper,
  # like if you have constraints or database-specific column types
  # config.active_record.schema_format = :sql

  # Activate observers that should always be running
  # Please note that observers generated using script/generate observer need to have an _observer suffix
  # config.active_record.observers = :cacher, :garbage_collector, :forum_observer

  # Add security related headers
  config.middleware.use SecurityHeaders
end


class ActiveRecord::Migration
  def self.dialect
    ActiveRecord::Base.configurations[ENV['RAILS_ENV']]['dialect']
  end

  def self.column_exists?(table_name, column_name)
    columns(table_name).any?{ |c| c.name == column_name.to_s }
  end

  def self.add_index(table_name, column_name, options = {})
    # ActiveRecord can generate index names longer than 30 characters, but that's
    # not supported by Oracle, the "Enterprise" database.
    # For this reason we force to set name of indexes.
    raise ArgumentError, 'Missing index name' unless options[:name]

    unless index_exists?(table_name, column_name, :name => options[:name])
      super(table_name, column_name, options)
    end
  end

  def self.remove_column(table_name, column_name)
    if column_exists?(table_name, column_name)
      super(table_name, column_name)
    end
  end

  def self.add_column(table_name, column_name, type, options = {})
    unless column_exists?(table_name, column_name)
      super(table_name, column_name, type, options)
    end
  end

  def self.add_varchar_index(table_name, column_name, options = {})
    if dialect()=='mysql' && !options[:length]
      # Index of varchar column is limited to 767 bytes on mysql (<= 255 UTF-8 characters)
      # See http://jira.sonarsource.com/browse/SONAR-4137 and
      # http://dev.mysql.com/doc/refman/5.6/en/innodb-restrictions.html
      options[:length]=255
    end
    add_index table_name, column_name, options
  end

  def self.execute_java_migration(classname)
    Java::OrgSonarServerUi::JRubyFacade.getInstance().databaseMigrator().executeMigration(classname)
  end

  def self.alter_to_big_primary_key(tablename)
    case dialect()
      when "postgre"
        execute "ALTER TABLE #{tablename} ALTER COLUMN id TYPE bigint"
      when "mysql"
        execute "ALTER TABLE #{tablename} CHANGE id id BIGINT AUTO_INCREMENT"
      when "h2"
        # not needed?
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
    case dialect()
      when "sqlserver"
        execute "DROP INDEX #{indexname} on #{tablename}" if indexname
        change_column(tablename, columnname, :big_integer, :null => true)
        execute "CREATE INDEX #{indexname} on #{tablename}(#{columnname})" if indexname
      else
        change_column(tablename, columnname, :big_integer, :null => true)
    end
  end

  # SONAR-4178
  def self.create_table(table_name, options = {})
    # Oracle constraint (see names of triggers and indices)
    raise ArgumentError, "Table name is too long: #{table_name}" if table_name.to_s.length>25

    super(table_name, options)
    create_id_trigger(table_name) if dialect()=='oracle' && options[:id] != false
  end

  def drop_table(table_name, options = {})
    super(table_name, options)
    drop_id_trigger(table_name) if dialect()=='oracle'
  end

  def self.create_id_trigger(table)
      execute_ddl("create trigger for table #{table}",

      %{CREATE OR REPLACE TRIGGER #{table}_idt
          BEFORE INSERT ON #{table}
          FOR EACH ROW
        BEGIN
           IF :new.id IS null THEN
             SELECT #{table}_seq.nextval INTO :new.id FROM dual;
           END IF;
        END;})
  end

  def self.drop_id_trigger(table)
      drop_trigger("#{table}_idt")
  end

  def self.drop_trigger(trigger_name)
    execute_ddl("drop trigger #{trigger_name}", "DROP TRIGGER #{trigger_name}")
  end

  def self.write(text="")
    # See migration.rb, the method write directly calls "puts"
    Java::OrgSlf4j::LoggerFactory::getLogger('DbMigration').info(text) if verbose
  end


  private

  def self.execute_ddl(message, ddl)
    say_with_time(message) do
      ActiveRecord::Base.connection.execute(ddl)
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

class ActionView::Base

  # Fix XSS - embed secure JSON in HTML
  # http://jfire.io/blog/2012/04/30/how-to-securely-bootstrap-json-in-a-rails-view/
  # Default implmentation of json_escape also removes double quote (") characters. It is documented to return invalid JSON !
  def json_escape(s)
    result = s.to_s.gsub('/', '\/')
    s.html_safe? ? result.html_safe : result
  end

  alias j json_escape
end


#
# other patches :
# - activerecord : fix Oracle bug when more than 1000 elements in IN clause. See lib/active_record/association_preload.rb
#   See https://github.com/rails/rails/issues/585
# - actionview NumberHelper, patch for number_with_precision()

require File.dirname(__FILE__) + '/../lib/java_ws_routing.rb'
require File.dirname(__FILE__) + '/../lib/database_version.rb'
DatabaseVersion.automatic_setup


#
#
# IMPORTANT NOTE
# Some changes have been done in activerecord-jdbc-adapter. Most of them relate to column types.
# All these changes are prefixed by the comment #sonar
#
#

# Increase size of form parameters
# See http://jira.sonarsource.com/browse/SONAR-5577
Rack::Utils.key_space_limit = 262144 # 4 times the default size

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

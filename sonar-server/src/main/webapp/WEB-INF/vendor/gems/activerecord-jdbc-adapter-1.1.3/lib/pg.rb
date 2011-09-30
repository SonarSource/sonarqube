# Stub library for postgresql -- allows Rails to load
# postgresql_adapter without error. Other than postgres-pr, there's no
# other way to use PostgreSQL on JRuby anyway, right? If you've
# installed ar-jdbc you probably want to use that to connect to pg.
#
# If by chance this library is installed in another Ruby and this file
# got required then we'll just continue to try to load the next pg.rb
# in the $LOAD_PATH.

unless defined?(JRUBY_VERSION)
  gem 'pg' if respond_to?(:gem)   # make sure pg gem is activated
  after_current_file = false
  $LOAD_PATH.each do |p|
    require_file = File.join(p, 'pg.rb')

    if File.expand_path(require_file) == File.expand_path(__FILE__)
      after_current_file = true
      next
    end

    if after_current_file && File.exist?(require_file)
      load require_file
      break
    end
  end
end

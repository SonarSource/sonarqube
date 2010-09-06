require File.expand_path(File.dirname(__FILE__) + "/lib/insert_routes.rb")
require 'digest/sha1'
class AuthenticatedGenerator < Rails::Generator::NamedBase
  default_options :skip_migration => false,
                  :skip_routes    => false,
                  :old_passwords  => false,
                  :include_activation => false

  attr_reader   :controller_name,
                :controller_class_path,
                :controller_file_path,
                :controller_class_nesting,
                :controller_class_nesting_depth,
                :controller_class_name,
                :controller_singular_name,
                :controller_plural_name,
                :controller_routing_name,                 # new_session_path
                :controller_routing_path,                 # /session/new
                :controller_controller_name,              # sessions
                :controller_file_name
  alias_method  :controller_table_name, :controller_plural_name
  attr_reader   :model_controller_name,
                :model_controller_class_path,
                :model_controller_file_path,
                :model_controller_class_nesting,
                :model_controller_class_nesting_depth,
                :model_controller_class_name,
                :model_controller_singular_name,
                :model_controller_plural_name,
                :model_controller_routing_name,           # new_user_path
                :model_controller_routing_path,           # /users/new
                :model_controller_controller_name         # users
  alias_method  :model_controller_file_name,  :model_controller_singular_name
  alias_method  :model_controller_table_name, :model_controller_plural_name

  def initialize(runtime_args, runtime_options = {})
    super

    @rspec = has_rspec?

    @controller_name = (args.shift || 'sessions').pluralize
    @model_controller_name = @name.pluralize

    # sessions controller
    base_name, @controller_class_path, @controller_file_path, @controller_class_nesting, @controller_class_nesting_depth = extract_modules(@controller_name)
    @controller_class_name_without_nesting, @controller_file_name, @controller_plural_name = inflect_names(base_name)
    @controller_singular_name = @controller_file_name.singularize
    if @controller_class_nesting.empty?
      @controller_class_name = @controller_class_name_without_nesting
    else
      @controller_class_name = "#{@controller_class_nesting}::#{@controller_class_name_without_nesting}"
    end
    @controller_routing_name  = @controller_singular_name
    @controller_routing_path  = @controller_file_path.singularize
    @controller_controller_name = @controller_plural_name

    # model controller
    base_name, @model_controller_class_path, @model_controller_file_path, @model_controller_class_nesting, @model_controller_class_nesting_depth = extract_modules(@model_controller_name)
    @model_controller_class_name_without_nesting, @model_controller_singular_name, @model_controller_plural_name = inflect_names(base_name)

    if @model_controller_class_nesting.empty?
      @model_controller_class_name = @model_controller_class_name_without_nesting
    else
      @model_controller_class_name = "#{@model_controller_class_nesting}::#{@model_controller_class_name_without_nesting}"
    end
    @model_controller_routing_name    = @table_name
    @model_controller_routing_path    = @model_controller_file_path
    @model_controller_controller_name = @model_controller_plural_name

    load_or_initialize_site_keys()

    if options[:dump_generator_attribute_names]
      dump_generator_attribute_names
    end
  end

  def manifest
    recorded_session = record do |m|
      # Check for class naming collisions.
      m.class_collisions controller_class_path,       "#{controller_class_name}Controller", # Sessions Controller
                                                      "#{controller_class_name}Helper"
      m.class_collisions model_controller_class_path, "#{model_controller_class_name}Controller", # Model Controller
                                                      "#{model_controller_class_name}Helper"
      m.class_collisions class_path,                  "#{class_name}", "#{class_name}Mailer", "#{class_name}MailerTest", "#{class_name}Observer"
      m.class_collisions [], 'AuthenticatedSystem', 'AuthenticatedTestHelper'

      # Controller, helper, views, and test directories.
      m.directory File.join('app/models', class_path)
      m.directory File.join('app/controllers', controller_class_path)
      m.directory File.join('app/controllers', model_controller_class_path)
      m.directory File.join('app/helpers', controller_class_path)
      m.directory File.join('app/views', controller_class_path, controller_file_name)
      m.directory File.join('app/views', class_path, "#{file_name}_mailer") if options[:include_activation]

      m.directory File.join('app/controllers', model_controller_class_path)
      m.directory File.join('app/helpers', model_controller_class_path)
      m.directory File.join('app/views', model_controller_class_path, model_controller_file_name)
      m.directory File.join('config/initializers')

      if @rspec
        m.directory File.join('spec/controllers', controller_class_path)
        m.directory File.join('spec/controllers', model_controller_class_path)
        m.directory File.join('spec/models', class_path)
        m.directory File.join('spec/helpers', model_controller_class_path)
        m.directory File.join('spec/fixtures', class_path)
        m.directory 'features'
        m.directory File.join('features', 'step_definitions')
      else
        m.directory File.join('test/functional', controller_class_path)
        m.directory File.join('test/functional', model_controller_class_path)
        m.directory File.join('test/unit', class_path)
        m.directory File.join('test/fixtures', class_path)
      end

      m.template 'model.rb',
                  File.join('app/models',
                            class_path,
                            "#{file_name}.rb")

      if options[:include_activation]
        %w( mailer observer ).each do |model_type|
          m.template "#{model_type}.rb", File.join('app/models',
                                               class_path,
                                               "#{file_name}_#{model_type}.rb")
        end
      end

      m.template 'controller.rb',
                  File.join('app/controllers',
                            controller_class_path,
                            "#{controller_file_name}_controller.rb")

      m.template 'model_controller.rb',
                  File.join('app/controllers',
                            model_controller_class_path,
                            "#{model_controller_file_name}_controller.rb")

      m.template 'authenticated_system.rb',
                  File.join('lib', 'authenticated_system.rb')

      m.template 'authenticated_test_helper.rb',
                  File.join('lib', 'authenticated_test_helper.rb')

      m.template 'site_keys.rb', site_keys_file

      if @rspec
        # RSpec Specs
        m.template  'spec/controllers/users_controller_spec.rb',
                    File.join('spec/controllers',
                              model_controller_class_path,
                              "#{model_controller_file_name}_controller_spec.rb")
        m.template  'spec/controllers/sessions_controller_spec.rb',
                    File.join('spec/controllers',
                              controller_class_path,
                              "#{controller_file_name}_controller_spec.rb")
        m.template  'spec/controllers/access_control_spec.rb',
                    File.join('spec/controllers',
                              controller_class_path,
                              "access_control_spec.rb")
        m.template  'spec/controllers/authenticated_system_spec.rb',
                    File.join('spec/controllers',
                              controller_class_path,
                              "authenticated_system_spec.rb")
        m.template  'spec/helpers/users_helper_spec.rb',
                    File.join('spec/helpers',
                              model_controller_class_path,
                              "#{table_name}_helper_spec.rb")
        m.template  'spec/models/user_spec.rb',
                    File.join('spec/models',
                              class_path,
                              "#{file_name}_spec.rb")
        m.template 'spec/fixtures/users.yml',
                    File.join('spec/fixtures',
                               class_path,
                              "#{table_name}.yml")

        # Cucumber features
        m.template  'features/step_definitions/ra_navigation_steps.rb',
         File.join('features/step_definitions/ra_navigation_steps.rb')
        m.template  'features/step_definitions/ra_response_steps.rb',
         File.join('features/step_definitions/ra_response_steps.rb')
        m.template  'features/step_definitions/ra_resource_steps.rb',
         File.join('features/step_definitions/ra_resource_steps.rb')
        m.template  'features/step_definitions/user_steps.rb',
         File.join('features/step_definitions/', "#{file_name}_steps.rb")
        m.template  'features/accounts.feature',
         File.join('features', 'accounts.feature')
        m.template  'features/sessions.feature',
         File.join('features', 'sessions.feature')
        m.template  'features/step_definitions/rest_auth_features_helper.rb',
         File.join('features', 'step_definitions', 'rest_auth_features_helper.rb')
        m.template  'features/step_definitions/ra_env.rb',
         File.join('features', 'step_definitions', 'ra_env.rb')

      else
        m.template 'test/functional_test.rb',
                    File.join('test/functional',
                              controller_class_path,
                              "#{controller_file_name}_controller_test.rb")
        m.template 'test/model_functional_test.rb',
                    File.join('test/functional',
                              model_controller_class_path,
                              "#{model_controller_file_name}_controller_test.rb")
        m.template 'test/unit_test.rb',
                    File.join('test/unit',
                              class_path,
                              "#{file_name}_test.rb")
        if options[:include_activation]
          m.template 'test/mailer_test.rb', File.join('test/unit', class_path, "#{file_name}_mailer_test.rb")
        end
        m.template 'spec/fixtures/users.yml',
                    File.join('test/fixtures',
                              class_path,
                              "#{table_name}.yml")
      end

      m.template 'helper.rb',
                  File.join('app/helpers',
                            controller_class_path,
                            "#{controller_file_name}_helper.rb")

      m.template 'model_helper.rb',
                  File.join('app/helpers',
                            model_controller_class_path,
                            "#{model_controller_file_name}_helper.rb")


      # Controller templates
      m.template 'login.html.erb',  File.join('app/views', controller_class_path, controller_file_name, "new.html.erb")
      m.template 'signup.html.erb', File.join('app/views', model_controller_class_path, model_controller_file_name, "new.html.erb")
      m.template '_model_partial.html.erb', File.join('app/views', model_controller_class_path, model_controller_file_name, "_#{file_name}_bar.html.erb")

      if options[:include_activation]
        # Mailer templates
        %w( activation signup_notification ).each do |action|
          m.template "#{action}.erb",
                     File.join('app/views', "#{file_name}_mailer", "#{action}.erb")
        end
      end

      unless options[:skip_migration]
        m.migration_template 'migration.rb', 'db/migrate', :assigns => {
          :migration_name => "Create#{class_name.pluralize.gsub(/::/, '')}"
        }, :migration_file_name => "create_#{file_path.gsub(/\//, '_').pluralize}"
      end
      unless options[:skip_routes]
        # Note that this fails for nested classes -- you're on your own with setting up the routes.
        m.route_resource  controller_singular_name
        m.route_resources model_controller_plural_name
        m.route_name('signup',   '/signup',   {:controller => model_controller_plural_name, :action => 'new'})
        m.route_name('register', '/register', {:controller => model_controller_plural_name, :action => 'create'})
        m.route_name('login',    '/login',    {:controller => controller_controller_name, :action => 'new'})
        m.route_name('logout',   '/logout',   {:controller => controller_controller_name, :action => 'destroy'})
      end
    end

    #
    # Post-install notes
    #
    action = File.basename($0) # grok the action from './script/generate' or whatever
    case action
    when "generate"
      puts "Ready to generate."
      puts ("-" * 70)
      puts "Once finished, don't forget to:"
      puts
      if options[:include_activation]
        puts "- Add an observer to config/environment.rb"
        puts "    config.active_record.observers = :#{file_name}_observer"
      end
      if options[:aasm]
        puts "- Install the acts_as_state_machine gem:"
        puts "    sudo gem sources -a http://gems.github.com (If you haven't already)"
        puts "    sudo gem install rubyist-aasm"
      elsif options[:stateful]
        puts "- Install the acts_as_state_machine plugin:"
        puts "    svn export http://elitists.textdriven.com/svn/plugins/acts_as_state_machine/trunk vendor/plugins/acts_as_state_machine"
      end
      puts "- Add routes to these resources. In config/routes.rb, insert routes like:"
      puts %(    map.signup '/signup', :controller => '#{model_controller_file_name}', :action => 'new')
      puts %(    map.login  '/login',  :controller => '#{controller_file_name}', :action => 'new')
      puts %(    map.logout '/logout', :controller => '#{controller_file_name}', :action => 'destroy')
      if options[:include_activation]
        puts %(    map.activate '/activate/:activation_code', :controller => '#{model_controller_file_name}', :action => 'activate', :activation_code => nil)
      end
      if options[:stateful]
        puts  "  and modify the map.resources :#{model_controller_file_name} line to include these actions:"
        puts  "    map.resources :#{model_controller_file_name}, :member => { :suspend => :put, :unsuspend => :put, :purge => :delete }"
      end
      puts
      puts ("-" * 70)
      puts
      if $rest_auth_site_key_from_generator.blank?
        puts "You've set a nil site key. This preserves existing users' passwords,"
        puts "but allows dictionary attacks in the unlikely event your database is"
        puts "compromised and your site code is not.  See the README for more."
      elsif $rest_auth_keys_are_new
        puts "We've create a new site key in #{site_keys_file}.  If you have existing"
        puts "user accounts their passwords will no longer work (see README). As always,"
        puts "keep this file safe but don't post it in public."
      else
        puts "We've reused the existing site key in #{site_keys_file}.  As always,"
        puts "keep this file safe but don't post it in public."
      end
      puts
      puts ("-" * 70)
    when "destroy"
      puts
      puts ("-" * 70)
      puts
      puts "Thanks for using restful_authentication"
      puts
      puts "Don't forget to comment out the observer line in environment.rb"
      puts "  (This was optional so it may not even be there)"
      puts "  # config.active_record.observers = :#{file_name}_observer"
      puts
      puts ("-" * 70)
      puts
    else
      puts "Didn't understand the action '#{action}' -- you might have missed the 'after running me' instructions."
    end

    #
    # Do the thing
    #
    recorded_session
  end

  def has_rspec?
    spec_dir = File.join(RAILS_ROOT, 'spec')
    options[:rspec] ||= (File.exist?(spec_dir) && File.directory?(spec_dir)) unless (options[:rspec] == false)
  end

  #
  # !! These must match the corresponding routines in by_password.rb !!
  #
  def secure_digest(*args)
    Digest::SHA1.hexdigest(args.flatten.join('--'))
  end
  def make_token
    secure_digest(Time.now, (1..10).map{ rand.to_s })
  end
  def password_digest(password, salt)
    digest = $rest_auth_site_key_from_generator
    $rest_auth_digest_stretches_from_generator.times do
      digest = secure_digest(digest, salt, password, $rest_auth_site_key_from_generator)
    end
    digest
  end

  #
  # Try to be idempotent:
  # pull in the existing site key if any,
  # seed it with reasonable defaults otherwise
  #
  def load_or_initialize_site_keys
    case
    when defined? REST_AUTH_SITE_KEY
      if (options[:old_passwords]) && ((! REST_AUTH_SITE_KEY.blank?) || (REST_AUTH_DIGEST_STRETCHES != 1))
        raise "You have a site key, but --old-passwords will overwrite it.  If this is really what you want, move the file #{site_keys_file} and re-run."
      end
      $rest_auth_site_key_from_generator         = REST_AUTH_SITE_KEY
      $rest_auth_digest_stretches_from_generator = REST_AUTH_DIGEST_STRETCHES
    when options[:old_passwords]
      $rest_auth_site_key_from_generator         = nil
      $rest_auth_digest_stretches_from_generator = 1
      $rest_auth_keys_are_new                    = true
    else
      $rest_auth_site_key_from_generator         = make_token
      $rest_auth_digest_stretches_from_generator = 10
      $rest_auth_keys_are_new                    = true
    end
  end
  def site_keys_file
    File.join("config", "initializers", "site_keys.rb")
  end

protected
  # Override with your own usage banner.
  def banner
    "Usage: #{$0} authenticated ModelName [ControllerName]"
  end

  def add_options!(opt)
    opt.separator ''
    opt.separator 'Options:'
    opt.on("--skip-migration",
      "Don't generate a migration file for this model")           { |v| options[:skip_migration] = v }
    opt.on("--include-activation",
      "Generate signup 'activation code' confirmation via email") { |v| options[:include_activation] = true }
    opt.on("--stateful",
      "Use acts_as_state_machine.  Assumes --include-activation") { |v| options[:include_activation] = options[:stateful] = true }
    opt.on("--aasm",
      "Use (gem) aasm.  Assumes --include-activation")            { |v| options[:include_activation] = options[:stateful] = options[:aasm] = true }
    opt.on("--rspec",
      "Force rspec mode (checks for RAILS_ROOT/spec by default)") { |v| options[:rspec] = true }
    opt.on("--no-rspec",
      "Force test (not RSpec mode")                               { |v| options[:rspec] = false }
    opt.on("--skip-routes",
      "Don't generate a resource line in config/routes.rb")       { |v| options[:skip_routes] = v }
    opt.on("--old-passwords",
      "Use the older password encryption scheme (see README)")    { |v| options[:old_passwords] = v }
    opt.on("--dump-generator-attrs",
      "(generator debug helper)")                                 { |v| options[:dump_generator_attribute_names] = v }
  end

  def dump_generator_attribute_names
    generator_attribute_names = [
      :table_name,
      :file_name,
      :class_name,
      :controller_name,
      :controller_class_path,
      :controller_file_path,
      :controller_class_nesting,
      :controller_class_nesting_depth,
      :controller_class_name,
      :controller_singular_name,
      :controller_plural_name,
      :controller_routing_name,                 # new_session_path
      :controller_routing_path,                 # /session/new
      :controller_controller_name,              # sessions
      :controller_file_name,
      :controller_table_name, :controller_plural_name,
      :model_controller_name,
      :model_controller_class_path,
      :model_controller_file_path,
      :model_controller_class_nesting,
      :model_controller_class_nesting_depth,
      :model_controller_class_name,
      :model_controller_singular_name,
      :model_controller_plural_name,
      :model_controller_routing_name,           # new_user_path
      :model_controller_routing_path,           # /users/new
      :model_controller_controller_name,        # users
      :model_controller_file_name,  :model_controller_singular_name,
      :model_controller_table_name, :model_controller_plural_name,
    ]
    generator_attribute_names.each do |attr|
      puts "%-40s %s" % ["#{attr}:", self.send(attr)]  # instance_variable_get("@#{attr.to_s}"
    end

  end
end

# ./script/generate authenticated FoonParent::Foon SporkParent::Spork -p --force --rspec --dump-generator-attrs
# table_name:                              foon_parent_foons
# file_name:                               foon
# class_name:                              FoonParent::Foon
# controller_name:                         SporkParent::Sporks
# controller_class_path:                   spork_parent
# controller_file_path:                    spork_parent/sporks
# controller_class_nesting:                SporkParent
# controller_class_nesting_depth:          1
# controller_class_name:                   SporkParent::Sporks
# controller_singular_name:                spork
# controller_plural_name:                  sporks
# controller_routing_name:                 spork
# controller_routing_path:                 spork_parent/spork
# controller_controller_name:              sporks
# controller_file_name:                    sporks
# controller_table_name:                   sporks
# controller_plural_name:                  sporks
# model_controller_name:                   FoonParent::Foons
# model_controller_class_path:             foon_parent
# model_controller_file_path:              foon_parent/foons
# model_controller_class_nesting:          FoonParent
# model_controller_class_nesting_depth:    1
# model_controller_class_name:             FoonParent::Foons
# model_controller_singular_name:          foons
# model_controller_plural_name:            foons
# model_controller_routing_name:           foon_parent_foons
# model_controller_routing_path:           foon_parent/foons
# model_controller_controller_name:        foons
# model_controller_file_name:              foons
# model_controller_singular_name:          foons
# model_controller_table_name:             foons
# model_controller_plural_name:            foons

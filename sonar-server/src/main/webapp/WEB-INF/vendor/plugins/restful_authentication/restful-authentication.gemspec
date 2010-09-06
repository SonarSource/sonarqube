# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = %q{restful-authentication}
  s.version = "1.1.1"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["RailsJedi", "Rick Olson"]
  s.date = %q{2008-07-04}
  s.description = %q{This widely-used plugin provides a foundation for securely managing user.}
  s.email = %q{railsjedi@gmail.com}
  s.extra_rdoc_files = ["README.textile"]
  s.files = ["CHANGELOG", "README.textile", "Rakefile", "TODO", "generators/authenticated/authenticated_generator.rb", "generators/authenticated/lib/insert_routes.rb", "generators/authenticated/templates/_model_partial.html.erb", "generators/authenticated/templates/activation.erb", "generators/authenticated/templates/authenticated_system.rb", "generators/authenticated/templates/authenticated_test_helper.rb", "generators/authenticated/templates/controller.rb", "generators/authenticated/templates/helper.rb", "generators/authenticated/templates/login.html.erb", "generators/authenticated/templates/mailer.rb", "generators/authenticated/templates/migration.rb", "generators/authenticated/templates/model.rb", "generators/authenticated/templates/model_controller.rb", "generators/authenticated/templates/model_helper.rb", "generators/authenticated/templates/model_helper_spec.rb", "generators/authenticated/templates/observer.rb", "generators/authenticated/templates/signup.html.erb", "generators/authenticated/templates/signup_notification.erb", "generators/authenticated/templates/site_keys.rb", "generators/authenticated/templates/spec/controllers/access_control_spec.rb", "generators/authenticated/templates/spec/controllers/authenticated_system_spec.rb", "generators/authenticated/templates/spec/controllers/sessions_controller_spec.rb", "generators/authenticated/templates/spec/controllers/users_controller_spec.rb", "generators/authenticated/templates/spec/fixtures/users.yml", "generators/authenticated/templates/spec/helpers/users_helper_spec.rb", "generators/authenticated/templates/spec/models/user_spec.rb", "generators/authenticated/templates/stories/rest_auth_stories.rb", "generators/authenticated/templates/stories/rest_auth_stories_helper.rb", "generators/authenticated/templates/stories/steps/ra_navigation_steps.rb", "generators/authenticated/templates/stories/steps/ra_resource_steps.rb", "generators/authenticated/templates/stories/steps/ra_response_steps.rb", "generators/authenticated/templates/stories/steps/user_steps.rb", "generators/authenticated/templates/stories/users/accounts.story", "generators/authenticated/templates/stories/users/sessions.story", "generators/authenticated/templates/test/functional_test.rb", "generators/authenticated/templates/test/mailer_test.rb", "generators/authenticated/templates/test/model_functional_test.rb", "generators/authenticated/templates/test/unit_test.rb", "generators/authenticated/USAGE", "init.rb", "lib/authentication/by_cookie_token.rb", "lib/authentication/by_password.rb", "lib/authentication.rb", "lib/authorization/aasm_roles.rb", "lib/authorization/stateful_roles.rb", "lib/authorization.rb", "lib/trustification/email_validation.rb", "lib/trustification.rb", "rails/init.rb"]
  s.has_rdoc = true
  s.homepage = %q{http://github.com/technoweenie/restful-authentication}
  s.rdoc_options = ["--main", "README.textile"]
  s.require_paths = ["lib"]
  s.rubygems_version = %q{1.3.0}
  s.summary = %q{Generates code for user login and authentication}

  if s.respond_to? :specification_version then
    current_version = Gem::Specification::CURRENT_SPECIFICATION_VERSION
    s.specification_version = 2

    if Gem::Version.new(Gem::RubyGemsVersion) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<rails>, ["~> 2.1.0"])
    else
      s.add_dependency(%q<rails>, ["~> 2.1.0"])
    end
  else
    s.add_dependency(%q<rails>, ["~> 2.1.0"])
  end
end

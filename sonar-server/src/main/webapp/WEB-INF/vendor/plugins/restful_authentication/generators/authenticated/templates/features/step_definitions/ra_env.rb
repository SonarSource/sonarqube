
Before do
  Fixtures.reset_cache
  fixtures_folder = File.join(RAILS_ROOT, 'spec', 'fixtures')
  Fixtures.create_fixtures(fixtures_folder, "users")
end

# Make visible for testing
ApplicationController.send(:public, :logged_in?, :current_user, :authorized?)

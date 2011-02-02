# Your secret key for verifying cookie session data integrity.
# If you change this key, all old sessions will become invalid!
# Make sure the secret is at least 30 characters and all random,
# no regular words or you'll be exposed to dictionary attacks.
ActionController::Base.session = {
  :key         => '_sonar_session',
  :secret      => 'bc2d855f87a32c43ce7c302b074b4271c58d024420437d6d85d03b19319e659f0c5bf3486b30480df43cda10bd95ad012956d77d3d35fc38edc639c232aacc11',
  :expire_after => 20.minutes
}

# Use the database for sessions instead of the cookie-based default,
# which shouldn't be used to store highly confidential information
# (create the session table with "rake db:sessions:create")
# ActionController::Base.session_store = :active_record_store

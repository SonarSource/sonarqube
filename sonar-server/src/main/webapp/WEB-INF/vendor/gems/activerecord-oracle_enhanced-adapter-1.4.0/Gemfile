source 'http://rubygems.org'

group :development do
  gem 'jeweler', '~> 1.5.1'
  gem 'rspec', '~> 2.4'

  if ENV['RAILS_GEM_VERSION']
    gem 'activerecord', "=#{ENV['RAILS_GEM_VERSION']}"
    gem 'actionpack', "=#{ENV['RAILS_GEM_VERSION']}"
    gem 'activesupport', "=#{ENV['RAILS_GEM_VERSION']}"
    case ENV['RAILS_GEM_VERSION']
    when /^2.0/
      gem 'composite_primary_keys', '=0.9.93'
    when /^2.1/
      gem 'composite_primary_keys', '=1.0.8'
    when /^2.2/
      gem 'composite_primary_keys', '=2.2.2'
    when /^2.3.3/
      gem 'composite_primary_keys', '=2.3.2'
    when /^3/
      gem 'railties', "=#{ENV['RAILS_GEM_VERSION']}"
    end
  else
    # uses local copy of Rails 3 and Arel gems
    ENV['RAILS_GEM_PATH'] ||= File.expand_path('../../rails', __FILE__)
    %w(activerecord activemodel activesupport actionpack railties).each do |gem_name|
      gem gem_name, :path => File.join(ENV['RAILS_GEM_PATH'], gem_name)
    end

    ENV['AREL_GEM_PATH'] ||= File.expand_path('../../arel', __FILE__)
    gem 'arel', :path => ENV['AREL_GEM_PATH']
  end

  gem 'ruby-plsql', '>=0.4.4'

  platforms :ruby do
    gem 'ruby-oci8', '~> 2.0.4'
  end

end

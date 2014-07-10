require 'rails/railtie'

module ::ArJdbc
  class Railtie < ::Rails::Railtie
    rake_tasks do
      load File.expand_path('../rake_tasks.rb', __FILE__)
    end
  end
end

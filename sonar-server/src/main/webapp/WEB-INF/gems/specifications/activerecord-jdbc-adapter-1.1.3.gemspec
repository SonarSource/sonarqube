# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "activerecord-jdbc-adapter"
  s.version = "1.1.3"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Nick Sieger, Ola Bini and JRuby contributors"]
  s.date = "2011-07-26"
  s.description = "activerecord-jdbc-adapter is a database adapter for Rails' ActiveRecord\ncomponent that can be used with JRuby[http://www.jruby.org/]. It allows use of\nvirtually any JDBC-compliant database with your JRuby on Rails application."
  s.email = "nick@nicksieger.com, ola.bini@gmail.com"
  s.extra_rdoc_files = ["History.txt", "Manifest.txt", "README.txt", "LICENSE.txt"]
  s.files = ["History.txt", "Manifest.txt", "README.txt", "LICENSE.txt"]
  s.homepage = "http://jruby-extras.rubyforge.org/activerecord-jdbc-adapter"
  s.rdoc_options = ["--main", "README.txt", "-SHN", "-f", "darkfish"]
  s.require_paths = ["lib"]
  s.rubyforge_project = "jruby-extras"
  s.rubygems_version = "1.8.23"
  s.summary = "JDBC adapter for ActiveRecord, for use within JRuby on Rails."

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rubyforge>, [">= 2.0.4"])
      s.add_development_dependency(%q<hoe>, [">= 2.9.4"])
    else
      s.add_dependency(%q<rubyforge>, [">= 2.0.4"])
      s.add_dependency(%q<hoe>, [">= 2.9.4"])
    end
  else
    s.add_dependency(%q<rubyforge>, [">= 2.0.4"])
    s.add_dependency(%q<hoe>, [">= 2.9.4"])
  end
end

# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "rails"
  s.version = "2.3.15"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["David Heinemeier Hansson"]
  s.date = "2013-01-08"
  s.description = "    Rails is a framework for building web-application using CGI, FCGI, mod_ruby, or WEBrick\n    on top of either MySQL, PostgreSQL, SQLite, DB2, SQL Server, or Oracle with eRuby- or Builder-based templates.\n"
  s.email = "david@loudthinking.com"
  s.executables = ["rails"]
  s.files = ["bin/rails"]
  s.homepage = "http://www.rubyonrails.org"
  s.rdoc_options = ["--exclude", "."]
  s.require_paths = ["lib"]
  s.rubyforge_project = "rails"
  s.rubygems_version = "1.8.23"
  s.summary = "Web-application framework with template engine, control-flow layer, and ORM."

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      # sonar - unused
      #s.add_runtime_dependency(%q<rake>, [">= 0.8.3"])
      # /sonar

      s.add_runtime_dependency(%q<activesupport>, ["= 2.3.15"])
      s.add_runtime_dependency(%q<activerecord>, ["= 2.3.15"])
      s.add_runtime_dependency(%q<actionpack>, ["= 2.3.15"])
      # sonar - unused
      #s.add_runtime_dependency(%q<actionmailer>, ["= 2.3.15"])
      #s.add_runtime_dependency(%q<activeresource>, ["= 2.3.15"])
      # /sonar
    else
      # sonar - unused
      #s.add_dependency(%q<rake>, [">= 0.8.3"])
      # /sonar
      s.add_dependency(%q<activesupport>, ["= 2.3.15"])
      s.add_dependency(%q<activerecord>, ["= 2.3.15"])
      s.add_dependency(%q<actionpack>, ["= 2.3.15"])
      # sonar - unused
      #s.add_dependency(%q<actionmailer>, ["= 2.3.15"])
      #s.add_dependency(%q<activeresource>, ["= 2.3.15"])
      # /sonar
    end
  else
    # sonar - unused
    #s.add_dependency(%q<rake>, [">= 0.8.3"])
    # /sonar
    s.add_dependency(%q<activesupport>, ["= 2.3.15"])
    s.add_dependency(%q<activerecord>, ["= 2.3.15"])
    s.add_dependency(%q<actionpack>, ["= 2.3.15"])
    # sonar - unused
    #s.add_dependency(%q<actionmailer>, ["= 2.3.15"])
    #s.add_dependency(%q<activeresource>, ["= 2.3.15"])
    # /sonar
  end
end

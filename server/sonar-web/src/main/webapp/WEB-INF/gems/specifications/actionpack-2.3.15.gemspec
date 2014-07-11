# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "actionpack"
  s.version = "2.3.15"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["David Heinemeier Hansson"]
  s.date = "2013-01-08"
  s.description = "Eases web-request routing, handling, and response as a half-way front, half-way page controller. Implemented with specific emphasis on enabling easy unit/integration testing that doesn't require a browser."
  s.email = "david@loudthinking.com"
  s.homepage = "http://www.rubyonrails.org"
  s.require_paths = ["lib"]
  s.requirements = ["none"]
  s.rubyforge_project = "actionpack"
  s.rubygems_version = "1.8.23"
  s.summary = "Web-flow and rendering framework putting the VC in MVC."

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<activesupport>, ["= 2.3.15"])
      s.add_runtime_dependency(%q<rack>, ["~> 1.1.3"])
    else
      s.add_dependency(%q<activesupport>, ["= 2.3.15"])
      s.add_dependency(%q<rack>, ["~> 1.1.3"])
    end
  else
    s.add_dependency(%q<activesupport>, ["= 2.3.15"])
    s.add_dependency(%q<rack>, ["~> 1.1.3"])
  end
end

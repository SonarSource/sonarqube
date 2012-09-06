# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "actionmailer"
  s.version = "2.3.14"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["David Heinemeier Hansson"]
  s.date = "2011-08-16"
  s.description = "Makes it trivial to test and deliver emails sent from a single service layer."
  s.email = "david@loudthinking.com"
  s.homepage = "http://www.rubyonrails.org"
  s.require_paths = ["lib"]
  s.requirements = ["none"]
  s.rubyforge_project = "actionmailer"
  s.rubygems_version = "1.8.15"
  s.summary = "Service layer for easy email delivery and testing."

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<actionpack>, ["= 2.3.14"])
    else
      s.add_dependency(%q<actionpack>, ["= 2.3.14"])
    end
  else
    s.add_dependency(%q<actionpack>, ["= 2.3.14"])
  end
end

# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "activesupport"
  s.version = "2.3.15"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["David Heinemeier Hansson"]
  s.date = "2013-01-08"
  s.description = "Utility library which carries commonly used classes and goodies from the Rails framework"
  s.email = "david@loudthinking.com"
  s.homepage = "http://www.rubyonrails.org"
  s.require_paths = ["lib"]
  s.rubyforge_project = "activesupport"
  s.rubygems_version = "1.8.23"
  s.summary = "Support and utility classes used by the Rails framework."

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end

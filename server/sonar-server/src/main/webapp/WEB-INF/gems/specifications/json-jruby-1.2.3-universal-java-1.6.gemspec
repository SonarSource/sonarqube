# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "json-jruby"
  s.version = "1.2.3"
  s.platform = "universal-java-1.6"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Daniel Luz"]
  s.date = "2010-03-14"
  s.email = "mernen+rubyforge@gmail.com"
  s.homepage = "http://rubyforge.org/projects/json-jruby/"
  s.require_paths = ["lib"]
  s.rubyforge_project = "json-jruby"
  s.rubygems_version = "1.8.23"
  s.summary = "A JSON implementation as a JRuby extension"

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end

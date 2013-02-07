# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "activeresource"
  s.version = "2.3.15"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["David Heinemeier Hansson"]
  s.date = "2013-01-08"
  s.description = "Wraps web resources in model classes that can be manipulated through XML over REST."
  s.email = "david@loudthinking.com"
  s.extra_rdoc_files = ["README"]
  s.files = ["README"]
  s.homepage = "http://www.rubyonrails.org"
  s.rdoc_options = ["--main", "README"]
  s.require_paths = ["lib"]
  s.rubyforge_project = "activeresource"
  s.rubygems_version = "1.8.23"
  s.summary = "Think Active Record for web resources."

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<activesupport>, ["= 2.3.15"])
    else
      s.add_dependency(%q<activesupport>, ["= 2.3.15"])
    end
  else
    s.add_dependency(%q<activesupport>, ["= 2.3.15"])
  end
end

# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "activerecord"
  s.version = "2.3.15"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["David Heinemeier Hansson"]
  s.date = "2013-01-08"
  s.description = "Implements the ActiveRecord pattern (Fowler, PoEAA) for ORM. It ties database tables and classes together for business objects, like Customer or Subscription, that can find, save, and destroy themselves without resorting to manual SQL."
  s.email = "david@loudthinking.com"
  s.extra_rdoc_files = ["README"]
  s.files = ["README"]
  s.homepage = "http://www.rubyonrails.org"
  s.rdoc_options = ["--main", "README"]
  s.require_paths = ["lib"]
  s.rubyforge_project = "activerecord"
  s.rubygems_version = "1.8.23"
  s.summary = "Implements the ActiveRecord pattern for ORM."

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

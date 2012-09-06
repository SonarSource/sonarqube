# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "fastercsv"
  s.version = "1.4.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["James Edward Gray II"]
  s.date = "2008-09-10"
  s.description = "FasterCSV is intended as a complete replacement to the CSV standard library. It is significantly faster and smaller while still being pure Ruby code. It also strives for a better interface."
  s.email = "james@grayproductions.net"
  s.extra_rdoc_files = ["AUTHORS", "COPYING", "README", "INSTALL", "TODO", "CHANGELOG", "LICENSE"]
  s.files = ["AUTHORS", "COPYING", "README", "INSTALL", "TODO", "CHANGELOG", "LICENSE"]
  s.homepage = "http://fastercsv.rubyforge.org"
  s.rdoc_options = ["--title", "FasterCSV Documentation", "--main", "README"]
  s.require_paths = ["lib"]
  s.rubyforge_project = "fastercsv"
  s.rubygems_version = "1.8.23"
  s.summary = "FasterCSV is CSV, but faster, smaller, and cleaner."

  if s.respond_to? :specification_version then
    s.specification_version = 2

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end

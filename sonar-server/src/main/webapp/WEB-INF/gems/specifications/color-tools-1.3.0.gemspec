# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "color-tools"
  s.version = "1.3.0"

  s.required_rubygems_version = nil if s.respond_to? :required_rubygems_version=
  s.authors = ["Austin Ziegler"]
  s.autorequire = "color"
  s.cert_chain = nil
  s.date = "2005-08-07"
  s.description = "color-tools is a Ruby library to provide RGB, CMYK, and other colourspace support to applications that require it. It also provides 152 named RGB colours. It offers 152 named RGB colours (184 with spelling variations) that are commonly supported and used in HTML, SVG, and X11 applications. A technique for generating a monochromatic contrasting palette is also included."
  s.email = "austin@rubyforge.org"
  s.extra_rdoc_files = ["README", "Install", "Changelog"]
  s.files = ["README", "Install", "Changelog"]
  s.homepage = "http://rubyforge.org/projects/ruby-pdf"
  s.rdoc_options = ["--title", "color-tools", "--main", "README", "--line-numbers"]
  s.require_paths = ["lib"]
  s.required_ruby_version = Gem::Requirement.new("> 0.0.0")
  s.rubyforge_project = "ruby-pdf"
  s.rubygems_version = "1.8.23"
  s.summary = "color-tools provides colour space definition and manpiulation as well as commonly named RGB colours."

  if s.respond_to? :specification_version then
    s.specification_version = 1

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end

Gem::Specification.new do |s|
  s.name = %q{color-tools}
  s.version = "1.3.0"

  s.specification_version = 1 if s.respond_to? :specification_version=

  s.required_rubygems_version = nil if s.respond_to? :required_rubygems_version=
  s.authors = ["Austin Ziegler"]
  s.autorequire = %q{color}
  s.cert_chain = nil
  s.date = %q{2005-08-07}
  s.description = %q{color-tools is a Ruby library to provide RGB, CMYK, and other colourspace support to applications that require it. It also provides 152 named RGB colours. It offers 152 named RGB colours (184 with spelling variations) that are commonly supported and used in HTML, SVG, and X11 applications. A technique for generating a monochromatic contrasting palette is also included.}
  s.email = %q{austin@rubyforge.org}
  s.extra_rdoc_files = ["README", "Install", "Changelog"]
  s.files = ["Changelog", "Install", "lib", "metaconfig", "pre-setup.rb", "Rakefile", "README", "setup.rb", "tests", "lib/color", "lib/color.rb", "lib/color/cmyk.rb", "lib/color/css.rb", "lib/color/grayscale.rb", "lib/color/hsl.rb", "lib/color/palette", "lib/color/palette.rb", "lib/color/rgb", "lib/color/rgb-colors.rb", "lib/color/rgb.rb", "lib/color/yiq.rb", "lib/color/palette/gimp.rb", "lib/color/palette/monocontrast.rb", "lib/color/rgb/metallic.rb", "tests/testall.rb", "tests/test_cmyk.rb", "tests/test_css.rb", "tests/test_gimp.rb", "tests/test_grayscale.rb", "tests/test_hsl.rb", "tests/test_monocontrast.rb", "tests/test_rgb.rb", "tests/test_yiq.rb"]
  s.has_rdoc = true
  s.homepage = %q{http://rubyforge.org/projects/ruby-pdf}
  s.rdoc_options = ["--title", "color-tools", "--main", "README", "--line-numbers"]
  s.require_paths = ["lib"]
  s.required_ruby_version = Gem::Requirement.new("> 0.0.0")
  s.rubyforge_project = %q{ruby-pdf}
  s.rubygems_version = %q{1.0.1}
  s.summary = %q{color-tools provides colour space definition and manpiulation as well as commonly named RGB colours.}
end

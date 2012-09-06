$:.unshift(File.expand_path('../lib', __FILE__))
require 'rack' # For Rack.release

Gem::Specification.new do |s|
  s.name            = "rack"
  s.version         = Rack.release
  s.platform        = Gem::Platform::RUBY
  s.summary         = "a modular Ruby webserver interface"

  s.description = <<-EOF
Rack provides minimal, modular and adaptable interface for developing
web applications in Ruby.  By wrapping HTTP requests and responses in
the simplest way possible, it unifies and distills the API for web
servers, web frameworks, and software in between (the so-called
middleware) into a single method call.

Also see http://rack.rubyforge.org.
EOF

  s.files           = Dir['{bin/*,contrib/*,example/*,lib/**/*}'] +
                        %w(COPYING KNOWN-ISSUES rack.gemspec RDOX README SPEC)
  s.bindir          = 'bin'
  s.executables     << 'rackup'
  s.require_path    = 'lib'
  s.has_rdoc        = true
  s.extra_rdoc_files = ['README', 'SPEC', 'KNOWN-ISSUES']
  s.test_files      = Dir['test/{test,spec}_*.rb']

  s.author          = 'Christian Neukirchen'
  s.email           = 'chneukirchen@gmail.com'
  s.homepage        = 'http://rack.rubyforge.org'
  s.rubyforge_project = 'rack'

  s.add_development_dependency 'test-spec'

  s.add_development_dependency 'activesupport', '< 2'
  s.add_development_dependency 'camping', '< 1.6'
  s.add_development_dependency 'fcgi'
  s.add_development_dependency 'memcache-client'
  s.add_development_dependency 'mongrel'
  s.add_development_dependency 'thin', '< 1.2' # since 1.2 or so, specs fail on Method
end

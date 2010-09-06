# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = %q{json-jruby}
  s.version = "1.2.3"
  s.platform = %q{universal-java-1.6}

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Daniel Luz"]
  s.date = %q{2010-03-14}
  s.email = %q{mernen+rubyforge@gmail.com}
  s.files = ["lib/json.rb", "lib/json/version.rb", "lib/json/ext.rb", "lib/json/pure.rb", "lib/json/common.rb", "lib/json/version.rb.orig", "lib/json/add/rails.rb", "lib/json/add/core.rb", "lib/json/ext/generator.jar", "lib/json/ext/parser.jar", "lib/json/pure/generator.rb.orig", "lib/json/pure/generator.rb", "lib/json/pure/parser.rb", "tests/test_json_generate.rb", "tests/test_json_unicode.rb", "tests/test_jjrb_offsets.rb", "tests/test_json_generate.rb.orig", "tests/test_json_addition.rb", "tests/test_json_fixtures.rb", "tests/test_json_encoding.rb", "tests/test_json_rails.rb", "tests/test_json.rb", "tests/fixtures/fail20.json", "tests/fixtures/fail6.json", "tests/fixtures/fail1.json", "tests/fixtures/fail14.json", "tests/fixtures/fail9.json", "tests/fixtures/pass26.json", "tests/fixtures/fail27.json", "tests/fixtures/fail19.json", "tests/fixtures/fail5.json", "tests/fixtures/fail10.json", "tests/fixtures/fail12.json", "tests/fixtures/fail3.json", "tests/fixtures/fail13.json", "tests/fixtures/fail24.json", "tests/fixtures/pass3.json", "tests/fixtures/fail23.json", "tests/fixtures/fail7.json", "tests/fixtures/pass1.json", "tests/fixtures/fail18.json", "tests/fixtures/pass17.json", "tests/fixtures/fail11.json", "tests/fixtures/fail21.json", "tests/fixtures/fail25.json", "tests/fixtures/fail4.json", "tests/fixtures/fail8.json", "tests/fixtures/fail2.json", "tests/fixtures/pass15.json", "tests/fixtures/fail22.json", "tests/fixtures/fail28.json", "tests/fixtures/pass2.json", "tests/fixtures/pass16.json"]
  s.homepage = %q{http://rubyforge.org/projects/json-jruby/}
  s.require_paths = ["lib"]
  s.rubyforge_project = %q{json-jruby}
  s.rubygems_version = %q{1.3.5}
  s.summary = %q{A JSON implementation as a JRuby extension}

  if s.respond_to? :specification_version then
    current_version = Gem::Specification::CURRENT_SPECIFICATION_VERSION
    s.specification_version = 3

    if Gem::Version.new(Gem::RubyGemsVersion) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end

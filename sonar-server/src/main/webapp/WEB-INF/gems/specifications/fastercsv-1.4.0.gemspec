Gem::Specification.new do |s|
  s.name = %q{fastercsv}
  s.version = "1.4.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["James Edward Gray II"]
  s.date = %q{2008-09-10}
  s.description = %q{FasterCSV is intended as a complete replacement to the CSV standard library. It is significantly faster and smaller while still being pure Ruby code. It also strives for a better interface.}
  s.email = %q{james@grayproductions.net}
  s.extra_rdoc_files = ["AUTHORS", "COPYING", "README", "INSTALL", "TODO", "CHANGELOG", "LICENSE"]
  s.files = ["lib/faster_csv.rb", "lib/fastercsv.rb", "test/tc_csv_parsing.rb", "test/tc_csv_writing.rb", "test/tc_data_converters.rb", "test/tc_encodings.rb", "test/tc_features.rb", "test/tc_headers.rb", "test/tc_interface.rb", "test/tc_row.rb", "test/tc_serialization.rb", "test/tc_speed.rb", "test/tc_table.rb", "test/ts_all.rb", "examples/csv_converters.rb", "examples/csv_filter.rb", "examples/csv_reading.rb", "examples/csv_table.rb", "examples/csv_writing.rb", "examples/shortcut_interface.rb", "test/test_data.csv", "examples/purchase.csv", "Rakefile", "setup.rb", "AUTHORS", "COPYING", "README", "INSTALL", "TODO", "CHANGELOG", "LICENSE"]
  s.has_rdoc = true
  s.homepage = %q{http://fastercsv.rubyforge.org}
  s.rdoc_options = ["--title", "FasterCSV Documentation", "--main", "README"]
  s.require_paths = ["lib"]
  s.rubyforge_project = %q{fastercsv}
  s.rubygems_version = %q{1.2.0}
  s.summary = %q{FasterCSV is CSV, but faster, smaller, and cleaner.}
  s.test_files = ["test/ts_all.rb"]

  if s.respond_to? :specification_version then
    current_version = Gem::Specification::CURRENT_SPECIFICATION_VERSION
    s.specification_version = 2

    if current_version >= 3 then
    else
    end
  else
  end
end

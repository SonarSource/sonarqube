require 'rake'
require 'rake/testtask'

task :default => [:java_compile, :test]

def java_classpath_arg # myriad of ways to discover JRuby classpath
  begin
    cpath  = Java::java.lang.System.getProperty('java.class.path').split(File::PATH_SEPARATOR)
    cpath += Java::java.lang.System.getProperty('sun.boot.class.path').split(File::PATH_SEPARATOR)
    jruby_cpath = cpath.compact.join(File::PATH_SEPARATOR)
  rescue => e
  end
  unless jruby_cpath
    jruby_cpath = ENV['JRUBY_PARENT_CLASSPATH'] || ENV['JRUBY_HOME'] &&
      FileList["#{ENV['JRUBY_HOME']}/lib/*.jar"].join(File::PATH_SEPARATOR)
  end
  jruby_cpath ? "-cp \"#{jruby_cpath}\"" : ""
end

desc "Compile the native Java code."
task :java_compile do
  pkg_classes = File.join(*%w(pkg classes))
  jar_name = File.join(*%w(lib jdbc_adapter jdbc_adapter_internal.jar))
  mkdir_p pkg_classes
  sh "javac -target 1.5 -source 1.5 -d pkg/classes #{java_classpath_arg} #{FileList['src/java/**/*.java'].join(' ')}"
  sh "jar cf #{jar_name} -C #{pkg_classes} ."
end
file "lib/jdbc_adapter/jdbc_adapter_internal.jar" => :java_compile

task :filelist do
  puts FileList['pkg/**/*'].inspect
end

if RUBY_PLATFORM =~ /java/
  # TODO: add more databases into the standard tests here.
  task :test => [:test_mysql, :test_jdbc, :test_derby, :test_hsqldb, :test_h2, :test_sqlite3]
else
  task :test => [:test_mysql]
end

FileList['drivers/*'].each do |d|
  next unless File.directory?(d)
  driver = File.basename(d)
  Rake::TestTask.new("test_#{driver}") do |t|
    files = FileList["test/#{driver}*test.rb"]
    if driver == "derby"
      files << 'test/activerecord/connection_adapters/type_conversion_test.rb'
    end
    t.ruby_opts << "-rjdbc/#{driver}"
    t.test_files = files
    t.libs << "test" << "#{d}/lib"
  end
end

Rake::TestTask.new(:test_jdbc) do |t|
  t.test_files = FileList['test/generic_jdbc_connection_test.rb', 'test/jndi_callbacks_test.rb']
  t.libs << 'test' << 'drivers/mysql/lib'
end

Rake::TestTask.new(:test_jndi) do |t|
  t.test_files = FileList['test/jndi_test.rb']
  t.libs << 'test' << 'drivers/derby/lib'
end

task :test_postgresql => [:test_postgres]
task :test_pgsql => [:test_postgres]

# Ensure oracle driver is on your classpath before launching rake
Rake::TestTask.new(:test_oracle) do |t|
  t.test_files = FileList['test/oracle_simple_test.rb']
  t.libs << 'test'
end

# Ensure DB2 driver is on your classpath before launching rake
Rake::TestTask.new(:test_db2) do |t|
  t.test_files = FileList['test/db2_simple_test.rb']
  t.libs << 'test'
end

# Ensure InterSystems CacheDB driver is on your classpath before launching rake
Rake::TestTask.new(:test_cachedb) do | t |
  t.test_files = FileList[ 'test/cachedb_simple_test.rb' ]
  t.libs << 'test'
end

# Ensure that the jTDS driver in on your classpath before launching rake
Rake::TestTask.new(:test_mssql) do | t |
  t.test_files = FileList[ 'test/mssql_simple_test.rb' ]
  t.libs << 'test'
end

# Ensure that the Informix driver is on your classpath before launching rake
Rake::TestTask.new(:test_informix) do |t|
  t.test_files = FileList[ 'test/informix_simple_test.rb' ]
  t.libs << 'test'
end

# Tests for JDBC adapters that don't require a database.
Rake::TestTask.new(:test_jdbc_adapters) do | t |
  t.test_files = FileList[ 'test/jdbc_adapter/jdbc_sybase_test.rb' ]
  t.libs << 'test'
end

MANIFEST = FileList["History.txt", "Manifest.txt", "README.txt", 
  "Rakefile", "LICENSE.txt", "lib/**/*.rb", "lib/jdbc_adapter/jdbc_adapter_internal.jar", "test/**/*.rb",
   "lib/**/*.rake", "src/**/*.java"]

file "Manifest.txt" => :manifest
task :manifest do
  File.open("Manifest.txt", "w") {|f| MANIFEST.each {|n| f << "#{n}\n"} }
end
Rake::Task['manifest'].invoke # Always regen manifest, so Hoe has up-to-date list of files

require File.dirname(__FILE__) + "/lib/jdbc_adapter/version"
begin
  require 'hoe'
  Hoe.new("activerecord-jdbc-adapter", JdbcAdapter::Version::VERSION) do |p|
    p.rubyforge_name = "jruby-extras"
    p.url = "http://jruby-extras.rubyforge.org/activerecord-jdbc-adapter"
    p.author = "Nick Sieger, Ola Bini and JRuby contributors"
    p.email = "nick@nicksieger.com, ola.bini@gmail.com"
    p.summary = "JDBC adapter for ActiveRecord, for use within JRuby on Rails."
    p.changes = p.paragraphs_of('History.txt', 0..1).join("\n\n")
    p.description = p.paragraphs_of('README.txt', 0...1).join("\n\n")
  end.spec.dependencies.delete_if { |dep| dep.name == "hoe" }
rescue LoadError
  puts "You really need Hoe installed to be able to package this gem"
rescue => e
  puts "ignoring error while loading hoe: #{e.to_s}"
end

def rake(*args)
  ruby "-S", "rake", *args
end

%w(test package install_gem release clean).each do |task|
  desc "Run rake #{task} on all available adapters and drivers"
  task "all:#{task}" => task
end

(Dir["drivers/*/Rakefile"] + Dir["adapters/*/Rakefile"]).each do |rakefile|
  dir = File.dirname(rakefile)
  prefix = dir.sub(%r{/}, ':')
  tasks = %w(package install_gem debug_gem clean)
  tasks << "test" if File.directory?(File.join(dir, "test"))
  tasks.each do |task|
    desc "Run rake #{task} on #{dir}"
    task "#{prefix}:#{task}" do
      Dir.chdir(dir) do
        rake task
      end
    end
    task "#{File.dirname(dir)}:#{task}" => "#{prefix}:#{task}"
    task "all:#{task}" => "#{prefix}:#{task}"
  end
  desc "Run rake release on #{dir}"
  task "#{prefix}:release" do
    Dir.chdir(dir) do
      version = nil
      if dir =~ /adapters/
        version = ENV['VERSION']
      else
        Dir["lib/**/*.rb"].each do |file|
          version ||= File.open(file) {|f| f.read =~ /VERSION = "([^"]+)"/ && $1}
        end
      end
      rake "release", "VERSION=#{version}"
    end
  end
  # Only release adapters synchronously with main release. Drivers are versioned
  # according to their JDBC driver versions.
  if dir =~ /adapters/
    task "adapters:release" => "#{prefix}:release"
    task "all:release" => "#{prefix}:release"
  end
end

require 'rake/clean'
CLEAN.include 'derby*', 'test.db.*','test/reports', 'test.sqlite3','lib/**/*.jar','manifest.mf', '*.log'

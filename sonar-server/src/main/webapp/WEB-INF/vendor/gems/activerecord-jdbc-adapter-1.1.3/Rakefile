require 'rake/testtask'
require 'rake/clean'
CLEAN.include 'derby*', 'test.db.*','test/reports', 'test.sqlite3','lib/**/*.jar','manifest.mf', '*.log'

task :default => [:java_compile, :test]

task :filelist do
  puts FileList['pkg/**/*'].inspect
end


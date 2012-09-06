#--
# Colour management with Ruby.
#
# Copyright 2005 Austin Ziegler
#   http://rubyforge.org/ruby-pdf/
#
#   Licensed under a MIT-style licence.
#
# $Id: pre-setup.rb,v 1.5 2005/08/08 02:53:16 austin Exp $
#++
# vim: sts=2 sw=2 ts=4 et ai tw=77

require 'rdoc/rdoc'

def build_rdoc(options)
  RDoc::RDoc.new.document(options)
rescue RDoc::RDocError => e
  $stderr.puts e.message
rescue Exception => e
  $stderr.puts "Couldn't build RDoc documentation\n#{e.message}"
end

def build_ri(files)
  RDoc::RDoc.new.document(files)
rescue RDoc::RDocError => e
  $stderr.puts e.message
rescue Exception => e
  $stderr.puts "Couldn't build Ri documentation\n#{e.message}"
end

def run_tests(test_list)
  return if test_list.empty?

  require 'test/unit/ui/console/testrunner'
  $:.unshift "lib"
  test_list.each do |test|
    next if File.directory?(test)
    require test
  end

  tests = []
  ObjectSpace.each_object { |o| tests << o if o.kind_of?(Class) }
  tests.delete_if { |o| !o.ancestors.include?(Test::Unit::TestCase) }
  tests.delete_if { |o| o == Test::Unit::TestCase }

  tests.each { |test| Test::Unit::UI::Console::TestRunner.run(test) }
  $:.shift
end

rdoc  = %w(--main README --line-numbers --title color-tools)
ri    = %w(--ri-site --merge)
dox   = %w(README Changelog lib)
build_rdoc rdoc + dox
build_ri ri + dox
run_tests Dir["tests/test_*.rb"]

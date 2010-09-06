require 'fileutils.rb'

class GenerateSources
  
  def initialize
     FileUtils.remove_dir "src/main/java", true
  end
  
	def template
	    if @template.nil?
			@template = ''
			f = File.open("template.java", "r") 
			f.each_line do |line|
  				@template += line
			end
	     end
	     @template
	end
	
	def sources(package_name, class_name)
	  template.gsub(/#PACKAGE/,package_name).gsub(/#CLASS/, class_name)
	end
	
	def generate(package_name, class_name)
	  content=sources(package_name, class_name)
	  dir="src/main/java/" + package_name.gsub(/\./, '/')
	  save(dir, "#{class_name}.java", content)
	end
	
	private
	def save(dir, filename,content)
		FileUtils::mkdir_p dir
		my_file = File.new("#{dir}/#{filename}", 'w')
		my_file<<content
		my_file.close
	end
end

packages_count=(ARGV.size>0 ? ARGV[0].to_i : 10)
classes_count=(ARGV.size>1 ? ARGV[1].to_i : 500)
generator=GenerateSources.new

for index_package in (1..packages_count)
  for index_class in (1..classes_count)
    generator.generate("org.sonar.tests.bigproject#{index_package}", "Class#{index_class}")
  end
end

puts "#{classes_count * packages_count} classes saved in #{packages_count} packages."
require 'fileutils.rb'

class SourceGenerator
  
  def initialize
     FileUtils.remove_dir "project/src/main/java", true
  end
  
	def template
	    if @template.nil?
			@template = ''
			f = File.open("project/Template.java", "r") 
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
	  dir="project/src/main/java/" + package_name.gsub(/\./, '/')
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


class ProjectDuplicator
  def initialize
    FileUtils.rm_f 'project/pom.xml'
    FileUtils.rm Dir.glob('project/pom.xml.*')
  end
 	def duplicate_pom(index)
	  pom=template().gsub(/#ID/,index.to_s)
	  filename=(index==1 ? "project/pom.xml" : "project/pom.xml.#{index}")
	  pom_file = File.new(filename, 'w')
		pom_file<<pom
		pom_file.close
	end
 
	def template
	  if @template.nil?
			@template = ''
			f = File.open("project/pom.template", "r") 
			f.each_line do |line|
  				@template += line
			end
	  end
	  @template
	end
	
end

projects_count=(ARGV.size>0 ? ARGV[0].to_i : 10)
packages_count=(ARGV.size>1 ? ARGV[1].to_i : 10)
classes_count=(ARGV.size>2 ? ARGV[2].to_i : 10)


source_generator=SourceGenerator.new
for index_package in (1..packages_count)
  for index_class in (1..classes_count)
    source_generator.generate("org.sonar.tests.volume#{index_package}", "Class#{index_class}")
  end
end
puts "#{classes_count * packages_count} classes saved in #{packages_count} packages."

duplicator=ProjectDuplicator.new
for index in (1..projects_count)
  puts "Generating project #{index}..."
  duplicator.duplicate_pom(index)
end
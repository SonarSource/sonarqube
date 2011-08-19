class JdbcGenerator < Rails::Generators::Base
  def self.source_root
    @source_root ||= File.expand_path('../../../../rails_generators/templates', __FILE__)
  end

  def create_jdbc_files
    directory '.', '.'
  end
end

def redefine_task(*args, &block)
  task_name = Hash === args.first ? args.first.keys[0] : args.first
  existing_task = Rake.application.lookup task_name
  if existing_task
    class << existing_task; public :instance_variable_set; end
    existing_task.instance_variable_set "@prerequisites", FileList[]
    existing_task.instance_variable_set "@actions", []
  end
  task(*args, &block)
end

namespace :db do
  redefine_task :create => :environment do
    create_database(ActiveRecord::Base.configurations[RAILS_ENV])
  end

  # hack to make the task work class must be commented to avoid a bug with rake
  #class << self; alias_method :previous_create_database, :create_database; end
  def create_database(config)
    begin
      ActiveRecord::Base.establish_connection(config)
      ActiveRecord::Base.connection
    rescue
      begin
        url = config['url']
        if url
          if url =~ /^(.*\/)/
            url = $1
          end
        end

        ActiveRecord::Base.establish_connection(config.merge({'database' => nil, 'url' => url}))
        ActiveRecord::Base.connection.create_database(config['database'])
        ActiveRecord::Base.establish_connection(config)
      rescue
        previous_create_database(config)
      end
    end
  end

  redefine_task :drop => :environment do
    config = ActiveRecord::Base.configurations[RAILS_ENV]
    begin
      ActiveRecord::Base.establish_connection(config)
      db = ActiveRecord::Base.connection.database_name
      ActiveRecord::Base.connection.drop_database(db)
    rescue
      drop_database(config)
    end
  end

  namespace :structure do
    redefine_task :dump => :environment do
      abcs = ActiveRecord::Base.configurations
      ActiveRecord::Base.establish_connection(abcs[RAILS_ENV])
      File.open("db/#{RAILS_ENV}_structure.sql", "w+") { |f| f << ActiveRecord::Base.connection.structure_dump }
      if ActiveRecord::Base.connection.supports_migrations?
        File.open("db/#{RAILS_ENV}_structure.sql", "a") { |f| f << ActiveRecord::Base.connection.dump_schema_information }
      end
    end
  end
  namespace :test do
    redefine_task :clone_structure => [ "db:structure:dump", "db:test:purge" ] do
      abcs = ActiveRecord::Base.configurations
      ActiveRecord::Base.establish_connection(:test)
      ActiveRecord::Base.connection.execute('SET foreign_key_checks = 0') if abcs["test"]["adapter"] =~ /mysql/i
      IO.readlines("db/#{RAILS_ENV}_structure.sql").join.split(";\n\n").each do |ddl|
        ActiveRecord::Base.connection.execute(ddl)
      end
    end

    redefine_task :purge => :environment do
      abcs = ActiveRecord::Base.configurations
      ActiveRecord::Base.establish_connection(:test)
      db = ActiveRecord::Base.connection.database_name
      ActiveRecord::Base.connection.recreate_database(db)
    end
  end
end

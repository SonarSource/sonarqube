# implementation idea taken from JDBC adapter
# added possibility to execute previously defined task (passed as argument to task block)
def redefine_task(*args, &block)
  task_name = Hash === args.first ? args.first.keys[0] : args.first
  existing_task = Rake.application.lookup task_name
  existing_actions = nil
  if existing_task
    class << existing_task; public :instance_variable_set, :instance_variable_get; end
    existing_task.instance_variable_set "@prerequisites", FileList[]
    existing_actions = existing_task.instance_variable_get "@actions"
    existing_task.instance_variable_set "@actions", []
  end
  task(*args) do
    block.call(existing_actions)
  end
end

# Creates database user with db:create
def create_database_with_oracle_enhanced(config)
  if config['adapter'] == 'oracle_enhanced'
    print "Please provide the SYSTEM password for your oracle installation\n>"
    system_password = $stdin.gets.strip
    ActiveRecord::Base.establish_connection(config.merge('username' => 'SYSTEM', 'password' => system_password))
    ActiveRecord::Base.connection.execute "DROP USER #{config['username']} CASCADE" rescue nil
    ActiveRecord::Base.connection.execute "CREATE USER #{config['username']} IDENTIFIED BY #{config['password']}"
    ActiveRecord::Base.connection.execute "GRANT unlimited tablespace TO #{config['username']}"
    ActiveRecord::Base.connection.execute "GRANT create session TO #{config['username']}"
    ActiveRecord::Base.connection.execute "GRANT create table TO #{config['username']}"
    ActiveRecord::Base.connection.execute "GRANT create sequence TO #{config['username']}"
  else
    create_database_without_oracle_enhanced(config)
  end
end
alias :create_database_without_oracle_enhanced :create_database
alias :create_database :create_database_with_oracle_enhanced

# Drops database user with db:drop
def drop_database_with_oracle_enhanced(config)
  if config['adapter'] == 'oracle_enhanced'
    print "Please provide the SYSTEM password for your oracle installation\n>"
    system_password = $stdin.gets.strip
    ActiveRecord::Base.establish_connection(config.merge('username' => 'SYSTEM', 'password' => system_password))
    ActiveRecord::Base.connection.execute "DROP USER #{config['username']} CASCADE"
  else
    drop_database_without_oracle_enhanced(config)
  end
end
alias :drop_database_without_oracle_enhanced :drop_database
alias :drop_database :drop_database_with_oracle_enhanced

namespace :db do

  namespace :structure do
    redefine_task :dump => :environment do |existing_actions|
      abcs = ActiveRecord::Base.configurations
      rails_env = defined?(Rails.env) ? Rails.env : RAILS_ENV
      if abcs[rails_env]['adapter'] == 'oracle_enhanced'
        ActiveRecord::Base.establish_connection(abcs[rails_env])
        File.open("db/#{rails_env}_structure.sql", "w+") { |f| f << ActiveRecord::Base.connection.structure_dump }
        if ActiveRecord::Base.connection.supports_migrations?
          File.open("db/#{rails_env}_structure.sql", "a") { |f| f << ActiveRecord::Base.connection.dump_schema_information }
        end
        if abcs[rails_env]['structure_dump'] == "db_stored_code"
           File.open("db/#{rails_env}_structure.sql", "a") { |f| f << ActiveRecord::Base.connection.structure_dump_db_stored_code }
        end
      else
        Array(existing_actions).each{|action| action.call}
      end
    end
  end

  namespace :test do
    redefine_task :clone_structure => [ "db:structure:dump", "db:test:purge" ] do |existing_actions|
      abcs = ActiveRecord::Base.configurations
      rails_env = defined?(Rails.env) ? Rails.env : RAILS_ENV
      if abcs[rails_env]['adapter'] == 'oracle_enhanced' && abcs['test']['adapter'] == 'oracle_enhanced'
        ActiveRecord::Base.establish_connection(:test)
        ActiveRecord::Base.connection.execute_structure_dump(File.read("db/#{rails_env}_structure.sql"))
      else
        Array(existing_actions).each{|action| action.call}
      end
    end

    redefine_task :purge => :environment do |existing_actions|
      abcs = ActiveRecord::Base.configurations
      if abcs['test']['adapter'] == 'oracle_enhanced'
        ActiveRecord::Base.establish_connection(:test)
        ActiveRecord::Base.connection.execute_structure_dump(ActiveRecord::Base.connection.full_drop)
        ActiveRecord::Base.connection.execute("PURGE RECYCLEBIN") rescue nil
      else
        Array(existing_actions).each{|action| action.call}
      end
    end

  end
end

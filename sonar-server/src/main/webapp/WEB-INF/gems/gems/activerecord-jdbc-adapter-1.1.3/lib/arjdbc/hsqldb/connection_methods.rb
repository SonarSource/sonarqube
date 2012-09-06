module ActiveRecord
  class Base
    class << self
      def hsqldb_connection(config)
        require "arjdbc/hsqldb"
        config[:url] ||= "jdbc:hsqldb:#{config[:database]}"
        config[:driver] ||= "org.hsqldb.jdbcDriver"
        embedded_driver(config)
      end

      alias_method :jdbchsqldb_connection, :hsqldb_connection
    end
  end
end

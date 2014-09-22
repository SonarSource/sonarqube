class ActiveRecord::Base
  class << self
    def mssql_connection(config)
      require "arjdbc/mssql"
      config[:host] ||= "localhost"
      config[:port] ||= 1433
      config[:driver] ||= "net.sourceforge.jtds.jdbc.Driver"

      url = "jdbc:jtds:sqlserver://#{config[:host]}:#{config[:port]}/#{config[:database]}"

      # Instance is often a preferrable alternative to port when dynamic ports are used.
      # If instance is specified then port is essentially ignored.
      url << ";instance=#{config[:instance]}" if config[:instance]

      # This will enable windows domain-based authentication and will require the JTDS native libraries be available.
      url << ";domain=#{config[:domain]}" if config[:domain]

      # AppName is shown in sql server as additional information against the connection.
      url << ";appname=#{config[:appname]}" if config[:appname]
      config[:url] ||= url

      if !config[:domain]
        config[:username] ||= "sa"
        config[:password] ||= ""
      end
      jdbc_connection(config)
    end
    alias_method :jdbcmssql_connection, :mssql_connection
  end
end

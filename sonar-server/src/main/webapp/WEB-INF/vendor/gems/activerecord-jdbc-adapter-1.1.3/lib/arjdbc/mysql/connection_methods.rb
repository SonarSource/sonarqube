# Don't need to load native mysql adapter
$LOADED_FEATURES << "active_record/connection_adapters/mysql_adapter.rb"
$LOADED_FEATURES << "active_record/connection_adapters/mysql2_adapter.rb"

class ActiveRecord::Base
  class << self
    def mysql_connection(config)
      require "arjdbc/mysql"
      config[:port] ||= 3306
      options = (config[:options] ||= {})
      options['zeroDateTimeBehavior'] ||= 'convertToNull'
      options['jdbcCompliantTruncation'] ||= 'false'
      options['useUnicode'] ||= 'true'
      options['characterEncoding'] = config[:encoding] || 'utf8'
      config[:url] ||= "jdbc:mysql://#{config[:host]}:#{config[:port]}/#{config[:database]}"
      config[:driver] ||= "com.mysql.jdbc.Driver"
      config[:adapter_class] = ActiveRecord::ConnectionAdapters::MysqlAdapter
      connection = jdbc_connection(config)
      ::ArJdbc::MySQL.kill_cancel_timer(connection.raw_connection)
      connection
    end
    alias_method :jdbcmysql_connection, :mysql_connection
    alias_method :mysql2_connection, :mysql_connection
  end
end



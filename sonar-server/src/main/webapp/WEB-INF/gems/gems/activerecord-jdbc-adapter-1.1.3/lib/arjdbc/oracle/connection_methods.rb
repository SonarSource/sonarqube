class ActiveRecord::Base
  class << self
    def oracle_connection(config)
      config[:port] ||= 1521
      config[:url] ||= "jdbc:oracle:thin:@#{config[:host]}:#{config[:port]}:#{config[:database]}"
      config[:driver] ||= "oracle.jdbc.driver.OracleDriver"
      jdbc_connection(config)
    end
  end
end


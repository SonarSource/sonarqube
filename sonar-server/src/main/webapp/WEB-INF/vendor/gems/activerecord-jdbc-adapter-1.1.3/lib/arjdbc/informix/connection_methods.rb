class ActiveRecord::Base
  class << self
    def informix_connection(config)
      config[:port] ||= 9088
      config[:url] ||= "jdbc:informix-sqli://#{config[:host]}:#{config[:port]}/#{config[:database]}:INFORMIXSERVER=#{config[:servername]}"
      config[:driver] = 'com.informix.jdbc.IfxDriver'
      jdbc_connection(config)
    end
  end
end

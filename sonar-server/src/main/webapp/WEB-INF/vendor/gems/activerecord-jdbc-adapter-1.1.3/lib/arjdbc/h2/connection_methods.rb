module ActiveRecord
  class Base
    class << self
      def h2_connection(config)
        config[:url] ||= "jdbc:h2:#{config[:database]}"
        config[:driver] ||= "org.h2.Driver"
        embedded_driver(config)
      end
      alias_method :jdbch2_connection, :h2_connection
    end
  end
end

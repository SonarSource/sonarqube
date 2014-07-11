module ActiveRecord
  module ConnectionAdapters
    class JdbcDriver
      def initialize(name)
        @name = name
        @driver = driver_class.new
      end

      def driver_class
        @driver_class ||= begin
          driver_class_const = (@name[0...1].capitalize + @name[1..@name.length]).gsub(/\./, '_')
          Jdbc::Mutex.synchronized do
            unless Jdbc.const_defined?(driver_class_const)
              driver_class_name = @name
              Jdbc.module_eval do
                java_import(driver_class_name) { driver_class_const }
              end
            end
          end
          driver_class = Jdbc.const_get(driver_class_const)
          raise "You must specify a driver for your JDBC connection" unless driver_class
          driver_class
        end
      end

      def connection(url, user, pass)
        # bypass DriverManager to get around problem with dynamically loaded jdbc drivers
        props = java.util.Properties.new
        props.setProperty("user", user)
        props.setProperty("password", pass)
        @driver.connect(url, props)
      end
    end
  end
end

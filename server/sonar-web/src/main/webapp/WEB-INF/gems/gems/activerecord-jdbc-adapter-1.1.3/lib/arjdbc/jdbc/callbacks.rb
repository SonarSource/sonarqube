module ActiveRecord
  module ConnectionAdapters
    module JdbcConnectionPoolCallbacks
      def self.included(base)
        if base.respond_to?(:set_callback) # Rails 3 callbacks
          base.set_callback :checkin, :after, :on_checkin
          base.set_callback :checkout, :before, :on_checkout
        else
          base.checkin :on_checkin
          base.checkout :on_checkout
        end
      end

      def self.needed?
        ActiveRecord::Base.respond_to?(:connection_pool)
      end

      def on_checkin
        # default implementation does nothing
      end

      def on_checkout
        # default implementation does nothing
      end
    end

    module JndiConnectionPoolCallbacks
      def self.prepare(adapter, conn)
        if ActiveRecord::Base.respond_to?(:connection_pool) && conn.jndi_connection?
          adapter.extend self
          conn.disconnect! # disconnect initial connection in JdbcConnection#initialize
        end
      end

      def on_checkin
        disconnect!
      end

      def on_checkout
        #sonar
        # Why do we try to reconnect ? It opens two connections instead of a single one.
        # Commenting the reconnection allows to have a single JDBC connection per HTTP request
        # https://jira.sonarsource.com/browse/SONAR-2784
        #reconnect!
        #/sonar
      end
    end
  end
end

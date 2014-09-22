require 'thread'

module ActionController
  class Reloader
    @@default_lock = Mutex.new
    cattr_accessor :default_lock

    class BodyWrapper
      def initialize(body, lock)
        @body = body
        #sonar
        # Development mode does not work with integration of Java web services (java_ws_controller.rb).
        #@lock = lock
        #/sonar
      end

      def close
        @body.close if @body.respond_to?(:close)
      ensure
        Dispatcher.cleanup_application
        #sonar
        # Development mode does not work with integration of Java web services (java_ws_controller.rb).
        #@lock.unlock
        #/sonar
      end

      def method_missing(*args, &block)
        @body.send(*args, &block)
      end

      def respond_to?(symbol, include_private = false)
        symbol == :close || @body.respond_to?(symbol, include_private)
      end
    end

    def self.run(lock = @@default_lock)
      #sonar
      # Development mode does not work with integration of Java web services (java_ws_controller.rb).
      #lock.lock
      #/sonar
      begin
        Dispatcher.reload_application
        status, headers, body = yield
        # We do not want to call 'cleanup_application' in an ensure block
        # because the returned Rack response body may lazily generate its data. This
        # is for example the case if one calls
        #
        #   render :text => lambda { ... code here which refers to application models ... }
        #
        # in an ActionController.
        #
        # Instead, we will want to cleanup the application code after the request is
        # completely finished. So we wrap the body in a BodyWrapper class so that
        # when the Rack handler calls #close during the end of the request, we get to
        # run our cleanup code.
        [status, headers, BodyWrapper.new(body, lock)]
      rescue Exception
        #sonar
        #lock.unlock
        #/sonar
        raise
      end
    end
  end
end

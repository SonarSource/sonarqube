module Rack
  module Auth
    class AbstractRequest

      def initialize(env)
        @env = env
      end

      def provided?
        !authorization_key.nil?
      end

      def parts
        @parts ||= @env[authorization_key].split(' ', 2)
      end

      def scheme
        @scheme ||=
          begin
            s = parts.first.downcase
            Rack::Auth.schemes.include?(s) ? s.to_sym : s
          end
      end

      def params
        @params ||= parts.last
      end


      private

      AUTHORIZATION_KEYS = ['HTTP_AUTHORIZATION', 'X-HTTP_AUTHORIZATION', 'X_HTTP_AUTHORIZATION']

      def authorization_key
        @authorization_key ||= AUTHORIZATION_KEYS.detect { |key| @env.has_key?(key) }
      end

    end

  end
end

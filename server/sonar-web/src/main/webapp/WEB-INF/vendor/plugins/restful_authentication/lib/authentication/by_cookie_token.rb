# -*- coding: utf-8 -*-
module Authentication
  module ByCookieToken
    # Stuff directives into including module 
    def self.included(recipient)
      recipient.extend(ModelClassMethods)
      recipient.class_eval do
        include ModelInstanceMethods
      end
    end

    #
    # Class Methods
    #
    module ModelClassMethods
    end # class methods

    #
    # Instance Methods
    #
    module ModelInstanceMethods
      def remember_token?
        (!remember_token.blank?) && 
          remember_token_expires_at && (Time.now.utc < remember_token_expires_at.utc)
      end

      # These create and unset the fields required for remembering users between browser closes
      def remember_me
        remember_me_for 2.weeks
      end

      def remember_me_for(time)
        remember_me_until time.from_now.utc
      end

      def remember_me_until(time)
        self.remember_token_expires_at = time
        self.remember_token            = self.class.make_token
        save(false)
      end

      # refresh token (keeping same expires_at) if it exists
      def refresh_token
        if remember_token?
          self.remember_token = self.class.make_token
          # Skip before_update as it populate dates columns with a long value (see migrations 752 to 754)
          send(:update_without_callbacks)
        end
      end

      # 
      # Deletes the server-side record of the authentication token.  The
      # client-side (browser cookie) and server-side (this remember_token) must
      # always be deleted together.
      #
      def forget_me
        self.remember_token_expires_at = nil
        self.remember_token            = nil
        save(false)
      end
    end # instance methods
  end

  module ByCookieTokenController
    # Stuff directives into including module 
    def self.included( recipient )
      recipient.extend( ControllerClassMethods )
      recipient.class_eval do
        include ControllerInstanceMethods
      end
    end

    #
    # Class Methods
    #
    module ControllerClassMethods
    end # class methods
    
    module ControllerInstanceMethods
    end # instance methods
  end
end


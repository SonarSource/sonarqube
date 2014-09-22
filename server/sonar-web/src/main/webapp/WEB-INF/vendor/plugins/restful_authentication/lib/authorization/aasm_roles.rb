module Authorization
  module AasmRoles
    unless Object.constants.include? "STATEFUL_ROLES_CONSTANTS_DEFINED"
      STATEFUL_ROLES_CONSTANTS_DEFINED = true # sorry for the C idiom
    end
    
    def self.included( recipient )
      recipient.extend( StatefulRolesClassMethods )
      recipient.class_eval do
        include StatefulRolesInstanceMethods
        include AASM
        aasm_column :state
        aasm_initial_state :initial => :pending
        aasm_state :passive
        aasm_state :pending, :enter => :make_activation_code
        aasm_state :active,  :enter => :do_activate
        aasm_state :suspended
        aasm_state :deleted, :enter => :do_delete

        aasm_event :register do
          transitions :from => :passive, :to => :pending, :guard => Proc.new {|u| !(u.crypted_password.blank? && u.password.blank?) }
        end
        
        aasm_event :activate do
          transitions :from => :pending, :to => :active 
        end
        
        aasm_event :suspend do
          transitions :from => [:passive, :pending, :active], :to => :suspended
        end
        
        aasm_event :delete do
          transitions :from => [:passive, :pending, :active, :suspended], :to => :deleted
        end

        aasm_event :unsuspend do
          transitions :from => :suspended, :to => :active,  :guard => Proc.new {|u| !u.activated_at.blank? }
          transitions :from => :suspended, :to => :pending, :guard => Proc.new {|u| !u.activation_code.blank? }
          transitions :from => :suspended, :to => :passive
        end
      end
    end

    module StatefulRolesClassMethods
    end # class methods

    module StatefulRolesInstanceMethods
      # Returns true if the user has just been activated.
      def recently_activated?
        @activated
      end
      def do_delete
        self.deleted_at = Time.now.utc
      end

      def do_activate
        @activated = true
        self.activated_at = Time.now.utc
        self.deleted_at = self.activation_code = nil
      end
    end # instance methods
  end
end

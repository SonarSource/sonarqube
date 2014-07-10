module ActiveRecord # :nodoc:
  # Represents exceptions that have propagated up through the JDBC API.
  class JDBCError < ActiveRecordError
    # The vendor code or error number that came from the database
    attr_accessor :errno

    # The full Java SQLException object that was raised
    attr_accessor :sql_exception
  end

  module ConnectionAdapters     # :nodoc:
    # Allows properly re-wrapping/re-defining methods that may already
    # be alias_method_chain'd.
    module ShadowCoreMethods
      def alias_chained_method(meth, feature, target)
        if instance_methods.include?("#{meth}_without_#{feature}")
          alias_method "#{meth}_without_#{feature}".to_sym, target
        else
          alias_method meth, target if meth != target
        end
      end
    end
  end
end

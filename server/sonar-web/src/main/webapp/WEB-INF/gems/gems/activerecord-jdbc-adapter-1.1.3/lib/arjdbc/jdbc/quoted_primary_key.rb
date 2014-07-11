module ArJdbc
  module QuotedPrimaryKeyExtension
    def self.extended(base)
      #       Rails 3 method           Rails 2 method
      meth = [:arel_attributes_values, :attributes_with_quotes].detect do |m|
        base.private_instance_methods.include?(m.to_s)
      end
      pk_hash_key = "self.class.primary_key"
      pk_hash_value = '"?"'
      if meth == :arel_attributes_values
        pk_hash_key = "self.class.arel_table[#{pk_hash_key}]"
        pk_hash_value = "Arel::SqlLiteral.new(#{pk_hash_value})"
      end
      if meth
        base.module_eval <<-PK, __FILE__, __LINE__
          alias :#{meth}_pre_pk :#{meth}
          def #{meth}(include_primary_key = true, *args) #:nodoc:
            aq = #{meth}_pre_pk(include_primary_key, *args)
            if connection.is_a?(ArJdbc::Oracle) || connection.is_a?(ArJdbc::Mimer)
              aq[#{pk_hash_key}] = #{pk_hash_value} if include_primary_key && aq[#{pk_hash_key}].nil?
            end
            aq
          end
        PK
      end
    end
  end
end

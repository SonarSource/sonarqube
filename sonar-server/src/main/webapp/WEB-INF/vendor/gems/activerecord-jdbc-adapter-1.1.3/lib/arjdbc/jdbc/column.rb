module ActiveRecord
  module ConnectionAdapters
    class JdbcColumn < Column
      attr_writer :limit, :precision

      def initialize(config, name, default, *args)
        call_discovered_column_callbacks(config)
        super(name,default_value(default),*args)
        init_column(name, default, *args)
      end

      def init_column(*args)
      end

      def default_value(val)
        val
      end

      def self.column_types
        # GH #25: reset the column types if the # of constants changed
        # since last call
        if ::ArJdbc.constants.size != driver_constants.size
          @driver_constants = nil
          @column_types = nil
        end
        @column_types ||= driver_constants.select {|c|
          c.respond_to? :column_selector }.map {|c|
          c.column_selector }.inject({}) {|h,val|
          h[val[0]] = val[1]; h }
      end

      def self.driver_constants
        @driver_constants ||= ::ArJdbc.constants.map {|c| ::ArJdbc.const_get c }
      end

      protected
      def call_discovered_column_callbacks(config)
        dialect = config[:dialect] || config[:driver]
        for reg, func in JdbcColumn.column_types
          if reg === dialect.to_s
            func.call(config,self)
          end
        end
      end
    end
  end
end

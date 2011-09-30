require 'delegate'

begin
  require "oci8"
rescue LoadError
  # OCI8 driver is unavailable.
  raise LoadError, "ERROR: ActiveRecord oracle_enhanced adapter could not load ruby-oci8 library. Please install ruby-oci8 gem."
end

# check ruby-oci8 version
required_oci8_version = [2, 0, 3]
oci8_version_ints = OCI8::VERSION.scan(/\d+/).map{|s| s.to_i}
if (oci8_version_ints <=> required_oci8_version) < 0
  raise LoadError, "ERROR: ruby-oci8 version #{OCI8::VERSION} is too old. Please install ruby-oci8 version #{required_oci8_version.join('.')} or later."
end

module ActiveRecord
  module ConnectionAdapters

    # OCI database interface for MRI
    class OracleEnhancedOCIConnection < OracleEnhancedConnection #:nodoc:

      def initialize(config)
        @raw_connection = OCI8EnhancedAutoRecover.new(config, OracleEnhancedOCIFactory)
        # default schema owner
        @owner = config[:username].to_s.upcase
      end

      def raw_oci_connection
        if @raw_connection.is_a? OCI8
          @raw_connection
        # ActiveRecord Oracle enhanced adapter puts OCI8EnhancedAutoRecover wrapper around OCI8
        # in this case we need to pass original OCI8 connection
        else
          @raw_connection.instance_variable_get(:@connection)
        end
      end

      def auto_retry
        @raw_connection.auto_retry if @raw_connection
      end

      def auto_retry=(value)
        @raw_connection.auto_retry = value if @raw_connection
      end

      def logoff
        @raw_connection.logoff
        @raw_connection.active = false
      end

      def commit
        @raw_connection.commit
      end

      def rollback
        @raw_connection.rollback
      end

      def autocommit?
        @raw_connection.autocommit?
      end

      def autocommit=(value)
        @raw_connection.autocommit = value
      end

      # Checks connection, returns true if active. Note that ping actively
      # checks the connection, while #active? simply returns the last
      # known state.
      def ping
        @raw_connection.ping
      rescue OCIException => e
        raise OracleEnhancedConnectionException, e.message
      end

      def active?
        @raw_connection.active?
      end

      def reset!
        @raw_connection.reset!
      rescue OCIException => e
        raise OracleEnhancedConnectionException, e.message
      end

      def exec(sql, *bindvars, &block)
        @raw_connection.exec(sql, *bindvars, &block)
      end

      def returning_clause(quoted_pk)
        " RETURNING #{quoted_pk} INTO :insert_id"
      end

      # execute sql with RETURNING ... INTO :insert_id
      # and return :insert_id value
      def exec_with_returning(sql)
        cursor = @raw_connection.parse(sql)
        cursor.bind_param(':insert_id', nil, Integer)
        cursor.exec
        cursor[':insert_id']
      ensure
        cursor.close rescue nil
      end

      def prepare(sql)
        Cursor.new(self, @raw_connection.parse(sql))
      end

      class Cursor
        def initialize(connection, raw_cursor)
          @connection = connection
          @raw_cursor = raw_cursor
        end

        def bind_param(position, value, col_type = nil)
          if value.nil?
            @raw_cursor.bind_param(position, nil, String)
          else
            case col_type
            when :text, :binary
              # ruby-oci8 cannot create CLOB/BLOB from ''
              lob_value = value == '' ? ' ' : value
              bind_type = col_type == :text ? OCI8::CLOB : OCI8::BLOB
              ora_value = bind_type.new(@connection.raw_oci_connection, lob_value)
              ora_value.size = 0 if value == ''
              @raw_cursor.bind_param(position, ora_value)
            when :raw
              @raw_cursor.bind_param(position, OracleEnhancedAdapter.encode_raw(value))
            else
              @raw_cursor.bind_param(position, value)
            end
          end
        end

        def bind_returning_param(position, bind_type)
          @raw_cursor.bind_param(position, nil, bind_type)
        end

        def exec
          @raw_cursor.exec
        end

        def exec_update
          @raw_cursor.exec
        end

        def get_col_names
          @raw_cursor.get_col_names
        end

        def fetch(options={})
          if row = @raw_cursor.fetch
            get_lob_value = options[:get_lob_value]
            row.map do |col|
              @connection.typecast_result_value(col, get_lob_value)
            end
          end
        end

        def get_returning_param(position, type)
          @raw_cursor[position]
        end

        def close
          @raw_cursor.close
        end

      end

      def select(sql, name = nil, return_column_names = false)
        cursor = @raw_connection.exec(sql)
        cols = []
        # Ignore raw_rnum_ which is used to simulate LIMIT and OFFSET
        cursor.get_col_names.each do |col_name|
          col_name = oracle_downcase(col_name)
          cols << col_name unless col_name == 'raw_rnum_'
        end
        # Reuse the same hash for all rows
        column_hash = {}
        cols.each {|c| column_hash[c] = nil}
        rows = []
        get_lob_value = !(name == 'Writable Large Object')

        while row = cursor.fetch
          hash = column_hash.dup

          cols.each_with_index do |col, i|
            hash[col] = typecast_result_value(row[i], get_lob_value)
          end

          rows << hash
        end

        return_column_names ? [rows, cols] : rows
      ensure
        cursor.close if cursor
      end

      def write_lob(lob, value, is_binary = false)
        lob.write value
      end
      
      def describe(name)
        # fall back to SELECT based describe if using database link
        return super if name.to_s.include?('@')
        quoted_name = OracleEnhancedAdapter.valid_table_name?(name) ? name : "\"#{name}\""
        @raw_connection.describe(quoted_name)
      rescue OCIException => e
        # fall back to SELECT which can handle synonyms to database links
        super
      end

      # Return OCIError error code
      def error_code(exception)
        case exception
        when OCIError
          exception.code
        else
          nil
        end
      end

      def typecast_result_value(value, get_lob_value)
        case value
        when Fixnum, Bignum
          value
        when String
          value
        when Float, BigDecimal
          # return Fixnum or Bignum if value is integer (to avoid issues with _before_type_cast values for id attributes)
          value == (v_to_i = value.to_i) ? v_to_i : value
        when OraNumber
          # change OraNumber value (returned in early versions of ruby-oci8 2.0.x) to BigDecimal
          value == (v_to_i = value.to_i) ? v_to_i : BigDecimal.new(value.to_s)
        when OCI8::LOB
          if get_lob_value
            data = value.read || ""     # if value.read returns nil, then we have an empty_clob() i.e. an empty string
            # In Ruby 1.9.1 always change encoding to ASCII-8BIT for binaries
            data.force_encoding('ASCII-8BIT') if data.respond_to?(:force_encoding) && value.is_a?(OCI8::BLOB)
            data
          else
            value
          end
        # ruby-oci8 1.0 returns OraDate
        # ruby-oci8 2.0 returns Time or DateTime
        when OraDate, Time, DateTime
          if OracleEnhancedAdapter.emulate_dates && date_without_time?(value)
            value.to_date
          else
            create_time_with_default_timezone(value)
          end
        else
          value
        end
      end

      private

      def date_without_time?(value)
        case value
        when OraDate
          value.hour == 0 && value.minute == 0 && value.second == 0
        else
          value.hour == 0 && value.min == 0 && value.sec == 0
        end
      end
      
      def create_time_with_default_timezone(value)
        year, month, day, hour, min, sec, usec = case value
        when Time
          [value.year, value.month, value.day, value.hour, value.min, value.sec, value.usec]
        when OraDate
          [value.year, value.month, value.day, value.hour, value.minute, value.second, 0]
        else
          [value.year, value.month, value.day, value.hour, value.min, value.sec, 0]
        end
        # code from Time.time_with_datetime_fallback
        begin
          Time.send(Base.default_timezone, year, month, day, hour, min, sec, usec)
        rescue
          offset = Base.default_timezone.to_sym == :local ? ::DateTime.local_offset : 0
          ::DateTime.civil(year, month, day, hour, min, sec, offset)
        end
      end

    end
    
    # The OracleEnhancedOCIFactory factors out the code necessary to connect and
    # configure an Oracle/OCI connection.
    class OracleEnhancedOCIFactory #:nodoc:
      def self.new_connection(config)
        # to_s needed if username, password or database is specified as number in database.yml file
        username = config[:username] && config[:username].to_s
        password = config[:password] && config[:password].to_s
        database = config[:database] && config[:database].to_s
        host, port = config[:host], config[:port]
        privilege = config[:privilege] && config[:privilege].to_sym
        async = config[:allow_concurrency]
        prefetch_rows = config[:prefetch_rows] || 100
        cursor_sharing = config[:cursor_sharing] || 'force'
        # get session time_zone from configuration or from TZ environment variable
        time_zone = config[:time_zone] || ENV['TZ']

        # connection using host, port and database name
        connection_string = if host || port
          host ||= 'localhost'
          host = "[#{host}]" if host =~ /^[^\[].*:/  # IPv6
          port ||= 1521
          "//#{host}:#{port}/#{database}"
        # if no host is specified then assume that
        # database parameter is TNS alias or TNS connection string
        else
          database
        end

        conn = OCI8.new username, password, connection_string, privilege
        conn.autocommit = true
        conn.non_blocking = true if async
        conn.prefetch_rows = prefetch_rows
        conn.exec "alter session set cursor_sharing = #{cursor_sharing}" rescue nil
        conn.exec "alter session set time_zone = '#{time_zone}'" unless time_zone.blank?

        # Initialize NLS parameters
        OracleEnhancedAdapter::DEFAULT_NLS_PARAMETERS.each do |key, default_value|
          value = config[key] || ENV[key.to_s.upcase] || default_value
          if value
            conn.exec "alter session set #{key} = '#{value}'"
          end
        end
        conn
      end
    end
    
    
  end
end



class OCI8 #:nodoc:

  class Cursor #:nodoc:
    if method_defined? :define_a_column
      # This OCI8 patch is required with the ruby-oci8 1.0.x or lower.
      # Set OCI8::BindType::Mapping[] to change the column type
      # when using ruby-oci8 2.0.

      alias :enhanced_define_a_column_pre_ar :define_a_column
      def define_a_column(i)
        case do_ocicall(@ctx) { @parms[i - 1].attrGet(OCI_ATTR_DATA_TYPE) }
        when 8;   @stmt.defineByPos(i, String, 65535) # Read LONG values
        when 187; @stmt.defineByPos(i, OraDate) # Read TIMESTAMP values
        when 108
          if @parms[i - 1].attrGet(OCI_ATTR_TYPE_NAME) == 'XMLTYPE'
            @stmt.defineByPos(i, String, 65535)
          else
            raise 'unsupported datatype'
          end
        else enhanced_define_a_column_pre_ar i
        end
      end
    end
  end

  if OCI8.public_method_defined?(:describe_table)
    # ruby-oci8 2.0 or upper

    def describe(name)
      info = describe_table(name.to_s)
      raise %Q{"DESC #{name}" failed} if info.nil?
      [info.obj_schema, info.obj_name]
    end
  else
    # ruby-oci8 1.0.x or lower

    # missing constant from oci8 < 0.1.14
    OCI_PTYPE_UNK = 0 unless defined?(OCI_PTYPE_UNK)

    # Uses the describeAny OCI call to find the target owner and table_name
    # indicated by +name+, parsing through synonynms as necessary. Returns
    # an array of [owner, table_name].
    def describe(name)
      @desc ||= @@env.alloc(OCIDescribe)
      @desc.attrSet(OCI_ATTR_DESC_PUBLIC, -1) if VERSION >= '0.1.14'
      do_ocicall(@ctx) { @desc.describeAny(@svc, name.to_s, OCI_PTYPE_UNK) } rescue raise %Q{"DESC #{name}" failed; does it exist?}
      info = @desc.attrGet(OCI_ATTR_PARAM)

      case info.attrGet(OCI_ATTR_PTYPE)
      when OCI_PTYPE_TABLE, OCI_PTYPE_VIEW
        owner      = info.attrGet(OCI_ATTR_OBJ_SCHEMA)
        table_name = info.attrGet(OCI_ATTR_OBJ_NAME)
        [owner, table_name]
      when OCI_PTYPE_SYN
        schema = info.attrGet(OCI_ATTR_SCHEMA_NAME)
        name   = info.attrGet(OCI_ATTR_NAME)
        describe(schema + '.' + name)
      else raise %Q{"DESC #{name}" failed; not a table or view.}
      end
    end
  end

end

# The OCI8AutoRecover class enhances the OCI8 driver with auto-recover and
# reset functionality. If a call to #exec fails, and autocommit is turned on
# (ie., we're not in the middle of a longer transaction), it will
# automatically reconnect and try again. If autocommit is turned off,
# this would be dangerous (as the earlier part of the implied transaction
# may have failed silently if the connection died) -- so instead the
# connection is marked as dead, to be reconnected on it's next use.
#:stopdoc:
class OCI8EnhancedAutoRecover < DelegateClass(OCI8) #:nodoc:
  attr_accessor :active #:nodoc:
  alias :active? :active #:nodoc:

  cattr_accessor :auto_retry
  class << self
    alias :auto_retry? :auto_retry #:nodoc:
  end
  @@auto_retry = false

  def initialize(config, factory) #:nodoc:
    @active = true
    @config = config
    @factory = factory
    @connection  = @factory.new_connection @config
    super @connection
  end

  # Checks connection, returns true if active. Note that ping actively
  # checks the connection, while #active? simply returns the last
  # known state.
  def ping #:nodoc:
    @connection.exec("select 1 from dual") { |r| nil }
    @active = true
  rescue
    @active = false
    raise
  end

  # Resets connection, by logging off and creating a new connection.
  def reset! #:nodoc:
    logoff rescue nil
    begin
      @connection = @factory.new_connection @config
      __setobj__ @connection
      @active = true
    rescue
      @active = false
      raise
    end
  end

  # ORA-00028: your session has been killed
  # ORA-01012: not logged on
  # ORA-03113: end-of-file on communication channel
  # ORA-03114: not connected to ORACLE
  # ORA-03135: connection lost contact
  LOST_CONNECTION_ERROR_CODES = [ 28, 1012, 3113, 3114, 3135 ] #:nodoc:

  # Adds auto-recovery functionality.
  #
  # See: http://www.jiubao.org/ruby-oci8/api.en.html#label-11
  def exec(sql, *bindvars, &block) #:nodoc:
    should_retry = self.class.auto_retry? && autocommit?

    begin
      @connection.exec(sql, *bindvars, &block)
    rescue OCIException => e
      raise unless e.is_a?(OCIError) && LOST_CONNECTION_ERROR_CODES.include?(e.code)
      @active = false
      raise unless should_retry
      should_retry = false
      reset! rescue nil
      retry
    end
  end

  # otherwise not working in Ruby 1.9.1
  if RUBY_VERSION =~ /^1\.9/
    def describe(name) #:nodoc:
      @connection.describe(name)
    end
  end

end
#:startdoc:

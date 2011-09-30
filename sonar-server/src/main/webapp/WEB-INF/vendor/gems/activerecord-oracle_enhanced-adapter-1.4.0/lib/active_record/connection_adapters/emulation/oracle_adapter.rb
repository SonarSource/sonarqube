class ActiveRecord::ConnectionAdapters::OracleAdapter < ActiveRecord::ConnectionAdapters::OracleEnhancedAdapter #:nodoc:
  def adapter_name
    'Oracle'
  end
end

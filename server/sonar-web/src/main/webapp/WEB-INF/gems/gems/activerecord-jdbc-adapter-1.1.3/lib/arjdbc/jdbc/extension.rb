module ArJdbc
  # Defines an AR-JDBC extension. An extension consists of a
  # declaration using this method and an ArJdbc::XYZ module that
  # contains implementation and overrides for methods in
  # ActiveRecord::ConnectionAdapters::AbstractAdapter. When you
  # declare your extension, you provide a block that detects when a
  # database configured to use the extension is present and loads the
  # necessary code for it. AR-JDBC will patch the code into the base
  # ActiveRecord::ConnectionAdapters::JdbcAdapter by extending an
  # instance of it with your extension module.
  #
  # +name+ should be a symbol that is the name of a module to be
  # defined under the +ArJdbc+ module.
  #
  # +block+ should be a one- or two-arity block that receives the
  # dialect name or driver class name as the first argument, and
  # optionally the whole database configuration hash as a second
  # argument.
  #
  # Example:
  #
  #     ArJdbc.extension :Frob do |name|
  #       if name =~ /frob/i
  #         # arjdbc/frob.rb should contain the implementation
  #         require 'arjdbc/frob'
  #         true
  #       end
  #     end
  def self.extension(name,&block)
    if const_defined?(name)
      mod = const_get(name)
    else
      mod = const_set(name, Module.new)
    end
    (class << mod; self; end).instance_eval do
      unless respond_to?(:adapter_matcher)
        define_method :adapter_matcher do |name, config|
          if block.arity == 1
            block.call(name) ? mod : false
          else
            block.call(name, config) ? mod : false
          end
        end
      end
    end
  end
end

require 'arel/engines/sql/compilers/ibm_db_compiler'

module Arel
  module SqlCompiler
    class DB2Compiler < IBM_DBCompiler
    end
  end
end


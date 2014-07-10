# arjdbc/discover.rb: Declare ArJdbc.extension modules in this file
# that loads a custom module and adapter.

module ::ArJdbc
  # Adapters built-in to AR are required up-front so we can override
  # the native ones
  require 'arjdbc/mysql'
  extension :MySQL do |name|
    name =~ /mysql/i
  end

  require 'arjdbc/postgresql'
  extension :PostgreSQL do |name|
    name =~ /postgre/i
  end

  require 'arjdbc/sqlite3'
  extension :SQLite3 do |name|
    name =~ /sqlite/i
  end

  # Other adapters are lazy-loaded
  extension :DB2 do |name, config|
    if name =~ /(db2|as400)/i && config[:url] !~ /^jdbc:derby:net:/
      require 'arjdbc/db2'
      true
    end
  end

  extension :Derby do |name|
    if name =~ /derby/i
      require 'arjdbc/derby'
      true
    end
  end

  extension :FireBird do |name|
    if name =~ /firebird/i
      require 'arjdbc/firebird'
      true
    end
  end

  extension :H2 do |name|
    if name =~ /\.h2\./i
      require 'arjdbc/h2'
      true
    end
  end

  extension :HSQLDB do |name|
    if name =~ /hsqldb/i
      require 'arjdbc/hsqldb'
      true
    end
  end

  extension :Informix do |name|
    if name =~ /informix/i
      require 'arjdbc/informix'
      true
    end
  end

  extension :Mimer do |name|
    if name =~ /mimer/i
      require 'arjdbc/mimer'
      true
    end
  end

  extension :MsSQL do |name|
    if name =~ /sqlserver|tds|Microsoft SQL/i
      require 'arjdbc/mssql'
      true
    end
  end

  extension :Oracle do |name|
    if name =~ /oracle/i
      require 'arjdbc/oracle'
      true
    end
  end

  extension :Sybase do |name|
    if name =~ /sybase|tds/i
      require 'arjdbc/sybase'
      true
    end
  end
end

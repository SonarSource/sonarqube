activerecord-jdbc-adapter is a database adapter for Rails' ActiveRecord
component that can be used with JRuby[http://www.jruby.org/]. It allows use of
virtually any JDBC-compliant database with your JRuby on Rails application.

== Databases

Activerecord-jdbc-adapter provides full or nearly full support for:
MySQL, PostgreSQL, SQLite3, Oracle, Microsoft SQL Server, DB2,
FireBird, Derby, HSQLDB, H2, and Informix.

Other databases will require testing and likely a custom configuration module.
Please join the activerecord-jdbc
mailing-lists[http://kenai.com/projects/activerecord-jdbc/lists] to help us discover
support for more databases.

== Using ActiveRecord JDBC

=== Inside Rails

To use activerecord-jdbc-adapter with JRuby on Rails:

1. Choose the adapter you wish to gem install. The following pre-packaged
adapters are available:

  * base jdbc (<tt>activerecord-jdbc-adapter</tt>). Supports all available databases via JDBC, but requires you to download and manually install the database vendor's JDBC driver .jar file.
  * mysql (<tt>activerecord-jdbcmysql-adapter</tt>)
  * postgresql (<tt>activerecord-jdbcpostgresql-adapter</tt>)
  * sqlite3 (<tt>activerecord-jdbcsqlite3-adapter</tt>)
  * derby (<tt>activerecord-jdbcderby-adapter</tt>)
  * hsqldb (<tt>activerecord-jdbchsqldb-adapter</tt>)
  * h2 (<tt>activerecord-jdbch2-adapter</tt>)
  * mssql (<tt>activerecord-jdbcmssql-adapter</tt>)

2a. For Rails 3, if you're generating a new application, use the
following command to generate your application:

   jruby -S rails new sweetapp -m http://jruby.org/rails3.rb

2b. Otherwise, you'll need to perform some extra configuration steps
to prepare your Rails application for JDBC.

If you're using Rails 3, you'll need to modify your Gemfile to use the
activerecord-jdbc-adapter gem under JRuby. Change your Gemfile to look
like the following (using sqlite3 as an example):

    if defined?(JRUBY_VERSION)
      gem 'activerecord-jdbc-adapter'
      gem 'jdbc-sqlite3'
    else
      gem 'sqlite3-ruby', :require => 'sqlite3'
    end

If you're using Rails 2:

    jruby script/generate jdbc

3. Configure your database.yml in the normal Rails style. 

Legacy configuration: If you use one of the convenience
'activerecord-jdbcXXX-adapter' adapters, you can still put a 'jdbc'
prefix in front of the database adapter name as below.

    development:
      adapter: jdbcmysql
      username: blog
      password:
      hostname: localhost
      database: weblog_development

For other databases, you'll need to know the database driver class and
URL. Example:

    development:
      adapter: jdbc
      username: blog
      password:
      driver: com.mysql.jdbc.Driver
      url: jdbc:mysql://localhost:3306/weblog_development

   For JNDI data sources, you may simply specify the JNDI location as follows
   (the adapter will be automatically detected):

    production:
      adapter: jdbc
      jndi: jdbc/mysqldb

=== Standalone, with ActiveRecord

1. Install the gem with JRuby:

    jruby -S gem install activerecord-jdbc-adapter

If you wish to use the adapter for a specific database, you can
install it directly and a driver gem will be installed as well:

    jruby -S gem install activerecord-jdbcderby-adapter

2. After this you can establish a JDBC connection like this:

    ActiveRecord::Base.establish_connection(
      :adapter => 'jdbcderby',
      :database => "db/my-database"
    )

or like this (but requires that you manually put the driver jar on the classpath):

    ActiveRecord::Base.establish_connection(
      :adapter => 'jdbc',
      :driver => 'org.apache.derby.jdbc.EmbeddedDriver',
      :url => 'jdbc:derby:test_ar;create=true'
    )

== Extending AR-JDBC

You can create your own extension to AR-JDBC for a JDBC-based database
that core AR-JDBC does not support. We've created an example project
for the Intersystems Cache database that you can examine as a
template. See the project for more information at the following URL:

  http://github.com/nicksieger/activerecord-cachedb-adapter

== Getting the source

The source for activerecord-jdbc-adapter is available using git.

  git clone git://github.com/nicksieger/activerecord-jdbc-adapter.git

== Feedback

Please file bug reports at
http://kenai.com/jira/browse/ACTIVERECORD_JDBC. If you're not sure if
something's a bug, feel free to pre-report it on the mailing lists.

== Project Info

* Mailing Lists: http://kenai.com/projects/activerecord-jdbc/lists
* Issues: http://kenai.com/jira/browse/ACTIVERECORD_JDBC
* Source:
 git://github.com/nicksieger/activerecord-jdbc-adapter.git
 git://kenai.com/activerecord-jdbc~main 

== Running AR-JDBC's Tests

Drivers for 6 open-source databases are included. Provided you have
MySQL installed, you can simply type <tt>jruby -S rake</tt> to run the
tests. A database named <tt>weblog_development</tt> is needed
beforehand with a connection user of "blog" and an empty password. You
alse need to grant "blog" create privileges on
'test_rake_db_create.*'.

If you also have PostgreSQL available, those tests will be run if the
`psql' executable can be found. Also ensure you have a database named
<tt>weblog_development</tt> and a user named "blog" and an empty
password.

If you want rails logging enabled during these test runs you can edit 
test/jdbc_common.rb and add the following line:

require 'db/logger'

== Running AR Tests

To run the current AR-JDBC sources with ActiveRecord, just use the
included "rails:test" task. Be sure to specify a driver and a path to
the ActiveRecord sources.

  jruby -S rake rails:test DRIVER=mysql RAILS=/path/activerecord_source_dir

== Authors

This project was written by Nick Sieger <nick@nicksieger.com> and Ola Bini
<olabini@gmail.com> with lots of help from the JRuby community.

== License

activerecord-jdbc-adapter is released under a BSD license. See the LICENSE file
included with the distribution for details.

Open-source driver gems for activerecord-jdbc-adapter are licensed under the
same license the database's drivers are licensed. See each driver gem's
LICENSE.txt file for details.

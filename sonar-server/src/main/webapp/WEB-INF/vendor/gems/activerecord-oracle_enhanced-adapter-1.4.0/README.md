activerecord-oracle_enhanced-adapter
====================================

Oracle enhanced adapter for ActiveRecord

DESCRIPTION
-----------

Oracle enhanced ActiveRecord adapter provides Oracle database access from Ruby on Rails applications. Oracle enhanced adapter can be used from Ruby on Rails versions 2.3.x and 3.x and it is working with Oracle database versions 10g and 11g.

INSTALLATION
------------

### Rails 3

When using Ruby on Rails version 3 then in Gemfile include

    gem 'activerecord-oracle_enhanced-adapter', '~> 1.4.0'

where instead of 1.4.0 you can specify any other desired version. It is recommended to specify version with `~>` which means that use specified version or later patch versions (in this example any later 1.4.x version but not 1.5.x version). Oracle enhanced adapter maintains API backwards compatibility during patch version upgrades and therefore it is safe to always upgrade to latest patch version.

If you would like to use latest adapter version from github then specify

    gem 'activerecord-oracle_enhanced-adapter', :git => 'git://github.com/rsim/oracle-enhanced.git'

If you are using MRI 1.8 or 1.9 Ruby implementation then you need to install ruby-oci8 gem as well as Oracle client, e.g. [Oracle Instant Client](http://www.oracle.com/technetwork/database/features/instant-client/index-097480.html). Include in Gemfile also ruby-oci8:

    gem 'ruby-oci8', '~> 2.0.6'

If you are using JRuby then you need to download latest [Oracle JDBC driver](http://www.oracle.com/technetwork/database/enterprise-edition/jdbc-112010-090769.html) - either ojdbc6.jar for Java 6 or ojdbc5.jar for Java 5. And copy this file to one of these locations:

  * in `./lib` directory of Rails application
  * in some directory which is in `PATH`
  * in `JRUBY_HOME/lib` directory
  * or include path to JDBC driver jar file in Java `CLASSPATH`

After specifying necessary gems in Gemfile run

    bundle install

to install the adapter (or later run `bundle update` to force updating to latest version).

### Rails 2.3

If you don't use Bundler in Rails 2 application then you need to specify gems in `config/environment.rb`, e.g.

    Rails::Initializer.run do |config|
      #...
      config.gem 'activerecord-oracle_enhanced-adapter', :lib => "active_record/connection_adapters/oracle_enhanced_adapter"
      config.gem 'ruby-oci8'
      #...
    end

But it is recommended to use Bundler for gem version management also for Rails 2.3 applications (search for instructions in Google).

### Without Rails and Bundler

If you want to use ActiveRecord and Oracle enhanced adapter without Rails and Bundler then install it just as a gem:

    gem install activerecord-oracle_enhanced-adapter

USAGE
-----

### Database connection

In Rails application `config/database.yml` use oracle_enhanced as adapter name, e.g.

    development:
      adapter: oracle_enhanced
      database: xe
      username: user
      password: secret

If `TNS_ADMIN` environment variable is pointing to directory where `tnsnames.ora` file is located then you can use TNS connection name in `database` parameter. Otherwise you can directly specify database host, port (defaults to 1521) and database name in the following way:

    development:
      adapter: oracle_enhanced
      host: localhost
      port: 1521
      database: xe
      username: user
      password: secret

or you can use Oracle specific format in `database` parameter:

    development:
      adapter: oracle_enhanced
      database: //localhost:1521/xe
      username: user
      password: secret

or you can even use Oracle specific TNS connection description:

    development:
      adapter: oracle_enhanced
      database: "(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=tcp)(HOST=localhost)(PORT=1521)))(CONNECT_DATA=(SERVICE_NAME=xe)))"
      username: user
      password: secret

If you deploy JRuby on Rails application in Java application server that supports JNDI connections then you can specify JNDI connection as well:

    development:
      adapter: oracle_enhanced
      jndi: "jdbc/jndi_connection_name"

You can find other available database.yml connection parameters in [oracle_enhanced_adapter.rb](/rsim/oracle-enhanced/blob/master/lib/active_record/connection_adapters/oracle_enhanced_adapter.rb). There are many NLS settings as well as some other Oracle session settings.

### Adapter settings

If you want to change Oracle enhanced adapter default settings then create initializer file e.g. `config/initializers/oracle.rb` specify there necessary defaults, e.g.:

    # It is recommended to set time zone in TZ environment variable so that the same timezone will be used by Ruby and by Oracle session
    ENV['TZ'] = 'UTC'

    ActiveSupport.on_load(:active_record) do
      ActiveRecord::ConnectionAdapters::OracleEnhancedAdapter.class_eval do
        # id columns and columns which end with _id will always be converted to integers
        self.emulate_integers_by_column_name = true
        # DATE columns which include "date" in name will be converted to Date, otherwise to Time
        self.emulate_dates_by_column_name = true
        # true and false will be stored as 'Y' and 'N'
        self.emulate_booleans_from_strings = true
        # start primary key sequences from 1 (and not 10000) and take just one next value in each session
        self.default_sequence_start_value = "1 NOCACHE INCREMENT BY 1"
        # other settings ...
      end
    end

In case of Rails 2 application you do not need to use `ActiveSupport.on_load(:active_record) do ... end` around settings code block.

See other adapter settings in [oracle_enhanced_adapter.rb](/rsim/oracle-enhanced/blob/master/lib/active_record/connection_adapters/oracle_enhanced_adapter.rb).

### Legacy schema support

If you want to put Oracle enhanced adapter on top of existing schema tables then there are several methods how to override ActiveRecord defaults, see example:

    class Employee < ActiveRecord::Base
      # specify schema and table name
      set_table_name "hr.hr_employees"
      # specify primary key name
      set_primary_key "employee_id"
      # specify sequence name
      set_sequence_name "hr.hr_employee_s"
      # set which DATE columns should be converted to Ruby Date
      set_date_columns :hired_on, :birth_date_on
      # set which DATE columns should be converted to Ruby Time
      set_datetime_columns :last_login_time
      # set which VARCHAR2 columns should be converted to true and false
      set_boolean_columns :manager, :active
      # set which columns should be ignored in ActiveRecord
      ignore_table_columns :attribute1, :attribute2
    end

You can also access remote tables over database link using

    set_table_name "hr_employees@db_link"

### Custom create, update and delete methods

If you have legacy schema and you are not allowed to do direct INSERTs, UPDATEs and DELETEs in legacy schema tables and need to use existing PL/SQL procedures for create, updated, delete operations then you should add `ruby-plsql` gem to your application and then define custom create, update and delete methods, see example:

    class Employee < ActiveRecord::Base
      # when defining create method then return ID of new record that will be assigned to id attribute of new object
      set_create_method do
        plsql.employees_pkg.create_employee(
          :p_first_name => first_name,
          :p_last_name => last_name,
          :p_employee_id => nil
        )[:p_employee_id]
      end
      set_update_method do
        plsql.employees_pkg.update_employee(
          :p_employee_id => id,
          :p_first_name => first_name,
          :p_last_name => last_name
        )
      end
      set_delete_method do
        plsql.employees_pkg.delete_employee(
          :p_employee_id => id
        )
      end
    end

In addition in `config/initializers/oracle.rb` initializer specify that ruby-plsql should use ActiveRecord database connection:

    plsql.activerecord_class = ActiveRecord::Base

### Oracle CONTEXT index support

Every edition of Oracle database includes [Oracle Text](http://www.oracle.com/technology/products/text/index.html) option for free which provides several full text indexing capabilities. Therefore in Oracle database case you don’t need external full text indexing and searching engines which can simplify your application deployment architecture.

To create simple single column index create migration with, e.g.

    add_context_index :posts, :title

and you can remove context index with

    remove_context_index :posts, :title

Include in class definition

    has_context_index

and then you can do full text search with

    Post.contains(:title, 'word')

You can create index on several columns (which will generate additional stored procedure for providing XML document with specified columns to indexer):

    add_context_index :posts, [:title, :body]

And you can search either in all columns or specify in which column you want to search (as first argument you need to specify first column name as this is the column which is referenced during index creation):

    Post.contains(:title, 'word')
    Post.contains(:title, 'word within title')
    Post.contains(:title, 'word within body')

See Oracle Text documentation for syntax that you can use in CONTAINS function in SELECT WHERE clause.

You can also specify some dummy main column name when creating multiple column index as well as specify to update index automatically after each commit (as otherwise you need to synchronize index manually or schedule periodic update):

    add_context_index :posts, [:title, :body], :index_column => :all_text, :sync => 'ON COMMIT'

    Post.contains(:all_text, 'word')

Or you can specify that index should be updated when specified columns are updated (e.g. in ActiveRecord you can specify to trigger index update when created_at or updated_at columns are updated). Otherwise index is updated only when main index column is updated.

    add_context_index :posts, [:title, :body], :index_column => :all_text,
      :sync => 'ON COMMIT', :index_column_trigger_on => [:created_at, :updated_at]

And you can even create index on multiple tables by providing SELECT statements which should be used to fetch necessary columns from related tables:

    add_context_index :posts,
      [:title, :body,
      # specify aliases always with AS keyword
      "SELECT comments.author AS comment_author, comments.body AS comment_body FROM comments WHERE comments.post_id = :id"
      ],
      :name => 'post_and_comments_index',
      :index_column => :all_text,
      :index_column_trigger_on => [:updated_at, :comments_count],
      :sync => 'ON COMMIT'

    # search in any table columns
    Post.contains(:all_text, 'word')
    # search in specified column
    Post.contains(:all_text, "aaa within title")
    Post.contains(:all_text, "bbb within comment_author")

### Oracle specific schema statements and data types

There are several additional schema statements and data types available that you can use in database migrations:

  * `add_foreign_key` and `remove_foreign_key` for foreign key definition (and they are also dumped in `db/schema.rb`)
  * `add_synonym` and `remove_synonym` for synonym definition (and they are also dumped in `db/schema.rb`)
  * You can create table with primary key trigger using `:primary_key_trigger => true` option for `create_table`
  * You can define columns with `raw` type which maps to Oracle's `RAW` type
  * You can add table and column comments with `:comment` option
  * On Oracle 11g you can define `virtual` columns with calculation formula in `:default` option
  * Default tablespaces can be specified for tables, indexes, clobs and blobs, for example:

        ActiveRecord::ConnectionAdapters::OracleEnhancedAdapter.default_tablespaces =
          {:clob => 'TS_LOB', :blob => 'TS_LOB', :index => 'TS_INDEX', :table => 'TS_DATA'}

TROUBLESHOOTING
---------------

### What to do if Oracle enhanced adapter is not working?

Please verify that

 1. Oracle Instant Client is installed correctly
    Can you connect to database using sqlnet?

 2. ruby-oci8 is installed correctly
    Try something like:

        ruby -rubygems -e "require 'oci8'; OCI8.new('username','password','database').exec('select * from dual') do |r| puts r.join(','); end"

    to verify that ruby-oci8 is working

 3. Verify that activerecord-oracle_enhanced-adapter is working from irb

        require 'rubygems'
        gem 'activerecord'
        gem 'activerecord-oracle_enhanced-adapter'
        require 'activerecord'
        ActiveRecord::Base.establish_connection(:adapter => "oracle_enhanced", :database => "database",:username => "user",:password => "password")

    and see if it is successful (use your correct database, username and password)

### What to do if Oracle enhanced adapter is not working with Phusion Passenger?

Oracle Instant Client and ruby-oci8 requires that several environment variables are set:

  * `LD_LIBRARY_PATH` (on Linux) or `DYLD_LIBRARY_PATH` (on Mac) should point to Oracle Instant Client directory (where Oracle client shared libraries are located)
  * `TNS_ADMIN` should point to directory where `tnsnames.ora` file is located
  * `NLS_LANG` should specify which territory and language NLS settings to use and which character set to use (e.g. `"AMERICAN_AMERICA.UTF8"`)

If this continues to throw "OCI Library Initialization Error (OCIError)", you might also need

  * `ORACLE_HOME` set to full Oracle client installation directory

When Apache with Phusion Passenger (mod_passenger or previously mod_rails) is used for Rails application deployment then by default Ruby is launched without environment variables that you have set in shell profile scripts (e.g. .profile). Therefore it is necessary to set environment variables in one of the following ways:

  * Create wrapper script as described in [Phusion blog](http://blog.phusion.nl/2008/12/16/passing-environment-variables-to-ruby-from-phusion-passenger) or [RayApps::Blog](http://blog.rayapps.com/2008/05/21/using-mod_rails-with-rails-applications-on-oracle)
  * Set environment variables in the file which is used by Apache before launching Apache worker processes - on Linux it typically is envvars file (look in apachectl or apache2ctl script where it is looking for envvars file) or /System/Library/LaunchDaemons/org.apache.httpd.plist on Mac OS X. See the following [discussion thread](http://groups.google.com/group/oracle-enhanced/browse_thread/thread/c5f64106569fadd0) for more hints.

RUNNING TESTS
-------------

See [RUNNING_TESTS.md](/rsim/oracle-enhanced/blob/master/RUNNING_TESTS.md) for information how to set up environment and run Oracle enhanced adapter unit tests.

LINKS
-----

* Source code: http://github.com/rsim/oracle-enhanced
* Bug reports / Feature requests / Pull requests: http://github.com/rsim/oracle-enhanced/issues
* Discuss at Oracle enhanced adapter group: http://groups.google.com/group/oracle-enhanced
* Blog posts about Oracle enhanced adapter can be found at http://blog.rayapps.com/category/oracle_enhanced

CONTRIBUTORS
------------

* Raimonds Simanovskis
* Jorge Dias
* James Wylder
* Rob Christie
* Nate Wieger
* Edgars Beigarts
* Lachlan Laycock
* toddwf
* Anton Jenkins
* Dave Smylie
* Alex Rothenberg
* Billy Reisinger
* David Blain
* Joe Khoobyar
* Edvard Majakari
* Beau Fabry
* Simon Chiang
* Peter Nyberg
* Dwayne Litzenberger
* Aaron Patterson
* Darcy Schultz
* Alexi Rahman
* Joeri Samson
* Luca Bernardo Ciddio
* Sam Baskinger
* Benjamin Ortega
* Yasuo Honda

LICENSE
-------

(The MIT License)

Copyright (c) 2008-2011 Graham Jenkins, Michael Schoen, Raimonds Simanovskis

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
'Software'), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
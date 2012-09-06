require 'test/spec'
require 'testrequest'
require 'rack/server'
require 'open3'

begin
require "mongrel"

context "rackup" do
  include TestRequest::Helpers

  def run_rackup(*args)
    options = args.last.is_a?(Hash) ? args.pop : {}
    flags   = args.first
    @host = options[:host] || "0.0.0.0"
    @port = options[:port] || 9292

    Dir.chdir("#{root}/test/rackup") do
      @in, @rackup, @err = Open3.popen3("#{ruby} -S #{rackup} #{flags}")
    end

    return if options[:port] == false

    # Wait until the server is available
    i = 0
    begin
      GET("/")
    rescue
      i += 1
      if i > 40
        Dir["#{root}/**/*.pid"].each {|f|
          Process.kill(9, File.read(f)) rescue nil
          File.delete(f)
        }
        raise "Server did not start"
      end
      sleep 0.05
      retry
    end
  end

  def output
    @rackup.read
  end

  after do
    # This doesn't actually return a response, so we rescue
    GET "/die" rescue nil

    Dir["#{root}/**/*.pid"].each do |file|
      Process.kill(9, File.read(file).strip.to_i) rescue nil
      File.delete(file)
    end

    File.delete("#{root}/log_output") if File.exist?("#{root}/log_output")
  end

  specify "rackup" do
    run_rackup
    response["PATH_INFO"].should.equal '/'
    response["test.$DEBUG"].should.be false
    response["test.$EVAL"].should.be nil
    response["test.$VERBOSE"].should.be false
    response["test.Ping"].should.be nil
    response["SERVER_SOFTWARE"].should.not =~ /webrick/
  end

  specify "rackup --help" do
    run_rackup "--help", :port => false
    output.should.match /--port/
  end

  specify "rackup --port" do
    run_rackup "--port 9000", :port => 9000
    response["SERVER_PORT"].should.equal "9000"
  end

  specify "rackup --debug" do
    run_rackup "--debug"
    response["test.$DEBUG"].should.be true
  end

  specify "rackup --eval" do
    run_rackup %{--eval "BUKKIT = 'BUKKIT'"}
    response["test.$EVAL"].should.equal "BUKKIT"
  end

  specify "rackup --warn" do
    run_rackup %{--warn}
    response["test.$VERBOSE"].should.be true
  end

  specify "rackup --include" do
    run_rackup %{--include /foo/bar}
    response["test.$LOAD_PATH"].should.include "/foo/bar"
  end

  specify "rackup --require" do
    run_rackup %{--require ping}
    response["test.Ping"].should.equal "constant"
  end

  specify "rackup --server" do
    run_rackup %{--server webrick}
    response["SERVER_SOFTWARE"].should =~ /webrick/i
  end

  specify "rackup --host" do
    run_rackup %{--host 127.0.0.1}, :host => "127.0.0.1"
    response["REMOTE_ADDR"].should.equal "127.0.0.1"
  end

  specify "rackup --daemonize --pid" do
    run_rackup "--daemonize --pid testing.pid"
    status.should.be 200
    @rackup.should.be.eof?
    Dir["#{root}/**/testing.pid"].should.not.be.empty?
  end

  specify "rackup --pid" do
    run_rackup %{--pid testing.pid}
    status.should.be 200
    Dir["#{root}/**/testing.pid"].should.not.be.empty?
  end

  specify "rackup --version" do
    run_rackup %{--version}, :port => false
    output.should =~ /Rack 1.1/
  end

  specify "rackup --env development includes lint" do
    run_rackup
    GET("/broken_lint")
    status.should.be 500
  end

  specify "rackup --env deployment does not include lint" do
    run_rackup %{--env deployment}
    GET("/broken_lint")
    status.should.be 200
  end

  specify "rackup --env none does not include lint" do
    run_rackup %{--env none}
    GET("/broken_lint")
    status.should.be 200
  end

  specify "rackup --env deployment does log" do
    run_rackup %{--env deployment}
    log = File.read(response["test.stderr"])
    log.should.be.empty?
  end

  specify "rackup --env none does not log" do
    run_rackup %{--env none}
    GET("/")
    log = File.read(response["test.stderr"])
    log.should.be.empty?
  end
end
rescue LoadError
  $stderr.puts "Skipping rackup --server tests (mongrel is required). `gem install thin` and try again."
end
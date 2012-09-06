require 'test/spec'
require 'stringio'

require 'rack/commonlogger'
require 'rack/lobster'
require 'rack/mock'

context "Rack::CommonLogger" do
  app = lambda { |env|
    [200,
     {"Content-Type" => "text/html", "Content-Length" => length.to_s},
     [obj]]}
  app_without_length = lambda { |env|
    [200,
     {"Content-Type" => "text/html"},
     []]}
  app_with_zero_length = lambda { |env|
    [200,
     {"Content-Type" => "text/html", "Content-Length" => "0"},
     []]}

  specify "should log to rack.errors by default" do
    res = Rack::MockRequest.new(Rack::CommonLogger.new(app)).get("/")

    res.errors.should.not.be.empty
    res.errors.should =~ /"GET \/ " 200 #{length} /
  end

  specify "should log to anything with +write+" do
    log = StringIO.new
    res = Rack::MockRequest.new(Rack::CommonLogger.new(app, log)).get("/")

    log.string.should =~ /"GET \/ " 200 #{length} /
  end

  specify "should log - content length if header is missing" do
    res = Rack::MockRequest.new(Rack::CommonLogger.new(app_without_length)).get("/")

    res.errors.should.not.be.empty
    res.errors.should =~ /"GET \/ " 200 - /
  end

  specify "should log - content length if header is zero" do
    res = Rack::MockRequest.new(Rack::CommonLogger.new(app_with_zero_length)).get("/")

    res.errors.should.not.be.empty
    res.errors.should =~ /"GET \/ " 200 - /
  end
  
  def length
    self.class.length
  end
  
  def self.length
    123
  end
  
  def self.obj
    "hello world"
  end
end

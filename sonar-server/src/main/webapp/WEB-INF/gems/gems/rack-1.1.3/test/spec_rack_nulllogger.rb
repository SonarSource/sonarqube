require 'rack/nulllogger'
require 'rack/lint'
require 'rack/mock'

context "Rack::NullLogger" do
  specify "acks as a nop logger" do
    app = lambda { |env|
      env['rack.logger'].warn "b00m"
      [200, {'Content-Type' => 'text/plain'}, ["Hello, World!"]]
    }
    Rack::NullLogger.new(app).call({})
  end
end

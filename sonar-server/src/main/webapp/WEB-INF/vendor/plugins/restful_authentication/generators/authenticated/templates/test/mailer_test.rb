require File.dirname(__FILE__) + '/../test_helper'
require '<%= file_name %>_mailer'

class <%= class_name %>MailerTest < Test::Unit::TestCase
  FIXTURES_PATH = File.dirname(__FILE__) + '/../fixtures'
  CHARSET = "utf-8"

  include ActionMailer::Quoting

  def setup
    ActionMailer::Base.delivery_method = :test
    ActionMailer::Base.perform_deliveries = true
    ActionMailer::Base.deliveries = []

    @expected = TMail::Mail.new
    @expected.set_content_type "text", "plain", { "charset" => CHARSET }
  end

  def test_dummy_test
    #do nothing
  end

  private
    def read_fixture(action)
      IO.readlines("#{FIXTURES_PATH}/<%= file_name %>_mailer/#{action}")
    end

    def encode(subject)
      quoted_printable(subject, CHARSET)
    end
end

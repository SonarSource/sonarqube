class FakeAppController < ApplicationController
  SECTION=Navigation::SECTION_HOME
  def index
    @title = 'Fake application'
  end

  def advanced
    render :partial => 'fake_app/advanced', :locals => {:properties => Property.find(:all)}
  end
end

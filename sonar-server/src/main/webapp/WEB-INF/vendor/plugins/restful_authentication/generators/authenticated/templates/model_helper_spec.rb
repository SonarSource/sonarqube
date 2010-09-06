require File.dirname(__FILE__) + '/../spec_helper'
include ApplicationHelper
include <%= model_controller_class_name %>Helper

describe "<%= model_controller_class_name %>Helper.link_to_<%= file_name %>" do
  before do
    @<%= file_name %> = <%= class_name %>.new({
        :name  => '<%= class_name %> Name',
        :login => '<%= file_name %>_name',
      })
    @<%= file_name %>.id = 1 # set non-attr_accessible specifically
  end

  it "should give an error on a nil <%= file_name %>" do
    lambda { link_to_<%= file_name %>(nil) }.should raise_error('Invalid <%= file_name %>')
  end

  it "should link to the given <%= file_name %>" do
    link_to_<%= file_name %>(@<%= file_name %>).should have_tag("a[href='/<%= table_name %>/1']")
  end

  it "should use given link text if :content_text is specified" do
    link_to_<%= file_name %>(@<%= file_name %>, :content_text => 'Hello there!').should have_tag("a", 'Hello there!')
  end

  it "should use the login as link text with no :content_method specified" do
    link_to_<%= file_name %>(@<%= file_name %>).should have_tag("a", '<%= file_name %>_name')
  end

  it "should use the name as link text with :content_method => :name" do
    link_to_<%= file_name %>(@<%= file_name %>, :content_method => :name).should have_tag("a", '<%= class_name %> Name')
  end

  it "should use the login as title with no :title_method specified" do
    link_to_<%= file_name %>(@<%= file_name %>).should have_tag("a[title='<%= file_name %>_name']")
  end

  it "should use the name as link title with :content_method => :name" do
    link_to_<%= file_name %>(@<%= file_name %>, :title_method => :name).should have_tag("a[title='<%= class_name %> Name']")
  end

  it "should have nickname as a class by default" do
    link_to_<%= file_name %>(@<%= file_name %>).should have_tag("a.nickname")
  end

  it "should take other classes and no longer have the nickname class" do
    result = link_to_<%= file_name %>(@<%= file_name %>, :class => 'foo bar')
    result.should have_tag("a.foo")
    result.should have_tag("a.bar")
  end
end

describe "<%= model_controller_class_name %>Helper.link_to_signin_with_IP" do
  before do
  end

  it "should link to the signin_path" do
    link_to_signin_with_IP().should have_tag("a[href='/signin']")
  end

  it "should use given link text if :content_text is specified" do
    link_to_signin_with_IP(:content_text => 'Hello there!').should have_tag("a", 'Hello there!')
  end

  it "should use the login as link text with no :content_method specified" do
    link_to_signin_with_IP().should have_tag("a", '0.0.0.0')
  end

  it "should use the ip address as title" do
    link_to_signin_with_IP().should have_tag("a[title='0.0.0.0']")
  end

  it "should by default be like school in summer and have no class" do
    link_to_signin_with_IP().should_not have_tag("a.nickname")
  end
  
  it "should have some class if you tell it to" do
    result = link_to_signin_with_IP(:class => 'foo bar')
    result.should have_tag("a.foo")
    result.should have_tag("a.bar")
  end
end

describe "<%= model_controller_class_name %>Helper.link_to_current_<%= file_name %>, When logged in" do
  fixtures :<%= table_name %>
  include AuthenticatedTestHelper
  before do
    login_as(:quentin)
  end

  it "should link to the given <%= file_name %>" do
    link_to_current_<%= file_name %>().should have_tag("a[href='/<%= table_name %>/1']")
  end

  it "should use given link text if :content_text is specified" do
    link_to_current_user(:content_text => 'Hello there!').should have_tag("a", 'Hello there!')
  end

  it "should use the login as link text with no :content_method specified" do
    link_to_current_user().should have_tag("a", 'quentin')
  end

  it "should use the name as link text with :content_method => :name" do
    link_to_current_user(:content_method => :name).should have_tag("a", 'Quentin')
  end

  it "should use the login as title with no :title_method specified" do
    link_to_current_user().should have_tag("a[title='quentin']")
  end

  it "should use the name as link title with :content_method => :name" do
    link_to_current_user(:title_method => :name).should have_tag("a[title='Quentin']")
  end

  it "should have nickname as a class" do
    link_to_current_user().should have_tag("a.nickname")
  end

  it "should take other classes and no longer have the nickname class" do
    result = link_to_current_user(:class => 'foo bar')
    result.should have_tag("a.foo")
    result.should have_tag("a.bar")
  end
end



describe "<%= model_controller_class_name %>Helper.link_to_current_user, When logged out" do
  include AuthenticatedTestHelper
  before do
  end

  it "should link to the signin_path" do
    link_to_current_user().should have_tag("a[href='/signin']")
  end

  it "should use given link text if :content_text is specified" do
    link_to_current_user(:content_text => 'Hello there!').should have_tag("a", 'Hello there!')
  end

  it "should use the IP address as link text with no :content_method specified" do
    link_to_current_user().should have_tag("a", '0.0.0.0')
  end

  it "should use the ip address as title" do
    link_to_current_user().should have_tag("a[title='0.0.0.0']")
  end

  it "should by default be like school in summer and have no class" do
    link_to_current_user().should_not have_tag("a.nickname")
  end

  it "should have some class if you tell it to" do
    result = link_to_current_user(:class => 'foo bar')
    result.should have_tag("a.foo")
    result.should have_tag("a.bar")
  end
end

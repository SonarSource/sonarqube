require File.dirname(__FILE__) + '<%= ('/..'*model_controller_class_nesting_depth) + '/../spec_helper' %>'
include ApplicationHelper
include <%= model_controller_class_name %>Helper
include AuthenticatedTestHelper

describe <%= model_controller_class_name %>Helper do
  before do
    @<%= file_name %> = mock_<%= file_name %>
  end
  
  describe "if_authorized" do 
    it "yields if authorized" do
      should_receive(:authorized?).with('a','r').and_return(true)
      if_authorized?('a','r'){|action,resource| [action,resource,'hi'] }.should == ['a','r','hi']
    end
    it "does nothing if not authorized" do
      should_receive(:authorized?).with('a','r').and_return(false)
      if_authorized?('a','r'){ 'hi' }.should be_nil
    end
  end
  
  describe "link_to_<%= file_name %>" do
    it "should give an error on a nil <%= file_name %>" do
      lambda { link_to_<%= file_name %>(nil) }.should raise_error('Invalid <%= file_name %>')
    end
    it "should link to the given <%= file_name %>" do
      should_receive(:<%= model_controller_routing_name.singularize %>_path).at_least(:once).and_return('/<%= model_controller_file_path %>/1')
      link_to_<%= file_name %>(@<%= file_name %>).should have_tag("a[href='/<%= model_controller_file_path %>/1']")
    end
    it "should use given link text if :content_text is specified" do
      link_to_<%= file_name %>(@<%= file_name %>, :content_text => 'Hello there!').should have_tag("a", 'Hello there!')
    end
    it "should use the login as link text with no :content_method specified" do
      link_to_<%= file_name %>(@<%= file_name %>).should have_tag("a", 'user_name')
    end
    it "should use the name as link text with :content_method => :name" do
      link_to_<%= file_name %>(@<%= file_name %>, :content_method => :name).should have_tag("a", 'U. Surname')
    end
    it "should use the login as title with no :title_method specified" do
      link_to_<%= file_name %>(@<%= file_name %>).should have_tag("a[title='user_name']")
    end
    it "should use the name as link title with :content_method => :name" do
      link_to_<%= file_name %>(@<%= file_name %>, :title_method => :name).should have_tag("a[title='U. Surname']")
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

  describe "link_to_login_with_IP" do
    it "should link to the login_path" do
      link_to_login_with_IP().should have_tag("a[href='/login']")
    end
    it "should use given link text if :content_text is specified" do
      link_to_login_with_IP('Hello there!').should have_tag("a", 'Hello there!')
    end
    it "should use the login as link text with no :content_method specified" do
      link_to_login_with_IP().should have_tag("a", '0.0.0.0')
    end
    it "should use the ip address as title" do
      link_to_login_with_IP().should have_tag("a[title='0.0.0.0']")
    end
    it "should by default be like school in summer and have no class" do
      link_to_login_with_IP().should_not have_tag("a.nickname")
    end
    it "should have some class if you tell it to" do
      result = link_to_login_with_IP(nil, :class => 'foo bar')
      result.should have_tag("a.foo")
      result.should have_tag("a.bar")
    end
    it "should have some class if you tell it to" do
      result = link_to_login_with_IP(nil, :tag => 'abbr')
      result.should have_tag("abbr[title='0.0.0.0']")
    end
  end

  describe "link_to_current_<%= file_name %>, When logged in" do
    before do
      stub!(:current_<%= file_name %>).and_return(@<%= file_name %>)
    end
    it "should link to the given <%= file_name %>" do
      should_receive(:<%= model_controller_routing_name.singularize %>_path).at_least(:once).and_return('/<%= model_controller_file_path %>/1')
      link_to_current_<%= file_name %>().should have_tag("a[href='/<%= model_controller_file_path %>/1']")
    end
    it "should use given link text if :content_text is specified" do
      link_to_current_<%= file_name %>(:content_text => 'Hello there!').should have_tag("a", 'Hello there!')
    end
    it "should use the login as link text with no :content_method specified" do
      link_to_current_<%= file_name %>().should have_tag("a", 'user_name')
    end
    it "should use the name as link text with :content_method => :name" do
      link_to_current_<%= file_name %>(:content_method => :name).should have_tag("a", 'U. Surname')
    end
    it "should use the login as title with no :title_method specified" do
      link_to_current_<%= file_name %>().should have_tag("a[title='user_name']")
    end
    it "should use the name as link title with :content_method => :name" do
      link_to_current_<%= file_name %>(:title_method => :name).should have_tag("a[title='U. Surname']")
    end
    it "should have nickname as a class" do
      link_to_current_<%= file_name %>().should have_tag("a.nickname")
    end
    it "should take other classes and no longer have the nickname class" do
      result = link_to_current_<%= file_name %>(:class => 'foo bar')
      result.should have_tag("a.foo")
      result.should have_tag("a.bar")
    end
  end

  describe "link_to_current_<%= file_name %>, When logged out" do
    before do
      stub!(:current_<%= file_name %>).and_return(nil)
    end
    it "should link to the login_path" do
      link_to_current_<%= file_name %>().should have_tag("a[href='/login']")
    end
    it "should use given link text if :content_text is specified" do
      link_to_current_<%= file_name %>(:content_text => 'Hello there!').should have_tag("a", 'Hello there!')
    end
    it "should use 'not signed in' as link text with no :content_method specified" do
      link_to_current_<%= file_name %>().should have_tag("a", 'not signed in')
    end
    it "should use the ip address as title" do
      link_to_current_<%= file_name %>().should have_tag("a[title='0.0.0.0']")
    end
    it "should by default be like school in summer and have no class" do
      link_to_current_<%= file_name %>().should_not have_tag("a.nickname")
    end
    it "should have some class if you tell it to" do
      result = link_to_current_<%= file_name %>(:class => 'foo bar')
      result.should have_tag("a.foo")
      result.should have_tag("a.bar")
    end
  end

end

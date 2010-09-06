require File.dirname(__FILE__) + '<%= ('/..'*controller_class_nesting_depth) + '/../spec_helper' %>'
  # Be sure to include AuthenticatedTestHelper in spec/spec_helper.rb instead
# Then, you can remove it from this and the units test.
include AuthenticatedTestHelper

#
# A test controller with and without access controls
#
class AccessControlTestController < ApplicationController
  before_filter :login_required, :only => :login_is_required
  def login_is_required
    respond_to do |format|
      @foo = { 'success' => params[:format]||'no fmt given'}
      format.html do render :text => "success"             end
      format.xml  do render :xml  => @foo, :status => :ok  end
      format.json do render :json => @foo, :status => :ok  end
    end
  end
  def login_not_required
    respond_to do |format|
      @foo = { 'success' => params[:format]||'no fmt given'}
      format.html do render :text => "success"             end
      format.xml  do render :xml  => @foo, :status => :ok  end
      format.json do render :json => @foo, :status => :ok  end
    end
  end
end

#
# Access Control
#

ACCESS_CONTROL_FORMATS = [
  ['',     "success"],
  ['xml',  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<hash>\n  <success>xml</success>\n</hash>\n"],
  ['json', "{\"success\": \"json\"}"],]
ACCESS_CONTROL_AM_I_LOGGED_IN = [
  [:i_am_logged_in,     :quentin],
  [:i_am_not_logged_in, nil],]
ACCESS_CONTROL_IS_LOGIN_REQD = [
  :login_not_required,
  :login_is_required,]

describe AccessControlTestController do
  fixtures        :<%= table_name %>
  before do
    # is there a better way to do this?
    ActionController::Routing::Routes.add_route '/login_is_required',           :controller => 'access_control_test',   :action => 'login_is_required'
    ActionController::Routing::Routes.add_route '/login_not_required',          :controller => 'access_control_test',   :action => 'login_not_required'
  end

  ACCESS_CONTROL_FORMATS.each do |format, success_text|
    ACCESS_CONTROL_AM_I_LOGGED_IN.each do |logged_in_status, <%= file_name %>_login|
      ACCESS_CONTROL_IS_LOGIN_REQD.each do |login_reqd_status|
        describe "requesting #{format.blank? ? 'html' : format}; #{logged_in_status.to_s.humanize} and #{login_reqd_status.to_s.humanize}" do
          before do
            logout_keeping_session!
            @<%= file_name %> = format.blank? ? login_as(<%= file_name %>_login) : authorize_as(<%= file_name %>_login)
            get login_reqd_status.to_s, :format => format
          end

          if ((login_reqd_status == :login_not_required) ||
              (login_reqd_status == :login_is_required && logged_in_status == :i_am_logged_in))
            it "succeeds" do
              response.should have_text(success_text)
              response.code.to_s.should == '200'
            end

          elsif (login_reqd_status == :login_is_required && logged_in_status == :i_am_not_logged_in)
            if ['html', ''].include? format
              it "redirects me to the log in page" do
                response.should redirect_to('/<%= controller_routing_path %>/new')
              end
            else
              it "returns 'Access denied' and a 406 (Access Denied) status code" do
                response.should have_text("HTTP Basic: Access denied.\n")
                response.code.to_s.should == '401'
              end
            end

          else
            warn "Oops no case for #{format} and #{logged_in_status.to_s.humanize} and #{login_reqd_status.to_s.humanize}"
          end
        end # describe

      end
    end
  end # cases

end

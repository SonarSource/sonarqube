module AuthenticatedTestHelper
  # Sets the current <%= file_name %> in the session from the <%= file_name %> fixtures.
  def login_as(<%= file_name %>)
    @request.session[:<%= file_name %>_id] = <%= file_name %> ? (<%= file_name %>.is_a?(<%= file_name.camelize %>) ? <%= file_name %>.id : <%= table_name %>(<%= file_name %>).id) : nil
  end

  def authorize_as(<%= file_name %>)
    @request.env["HTTP_AUTHORIZATION"] = <%= file_name %> ? ActionController::HttpAuthentication::Basic.encode_credentials(<%= table_name %>(<%= file_name %>).login, 'monkey') : nil
  end
  
<% if options[:rspec] -%>
  # rspec
  def mock_<%= file_name %>
    <%= file_name %> = mock_model(<%= class_name %>, :id => 1,
      :login  => 'user_name',
      :name   => 'U. Surname',
      :to_xml => "<%= class_name %>-in-XML", :to_json => "<%= class_name %>-in-JSON", 
      :errors => [])
    <%= file_name %>
  end  
<% end -%>
end

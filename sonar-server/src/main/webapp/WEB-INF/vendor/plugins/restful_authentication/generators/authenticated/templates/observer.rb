class <%= class_name %>Observer < ActiveRecord::Observer
  def after_create(<%= file_name %>)
    <%= class_name %>Mailer.deliver_signup_notification(<%= file_name %>)
  end

  def after_save(<%= file_name %>)
  <% if options[:include_activation] %>
    <%= class_name %>Mailer.deliver_activation(<%= file_name %>) if <%= file_name %>.recently_activated?
  <% end %>
  end
end

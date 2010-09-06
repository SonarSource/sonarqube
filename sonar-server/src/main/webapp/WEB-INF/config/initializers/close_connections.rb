require 'action_controller/dispatcher'  
ActionController::Dispatcher.after_dispatch do
  ActiveRecord::Base.clear_active_connections!
end
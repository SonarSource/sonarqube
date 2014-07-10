if defined?(ActionController::Request)
  ActionController::Request.send :include, HttpAcceptLanguage
elsif defined?(ActionController::AbstractRequest)
  ActionController::AbstractRequest.send :include, HttpAcceptLanguage
else
  ActionController::CgiRequest.send :include, HttpAcceptLanguage
end

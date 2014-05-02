define [
  'backbone'
], (
  Backbone
) ->

  class WebServiceActionResponse extends Backbone.Model

  	url: ->
      "#{baseUrl}/api/webservices/responseExample?controller=#{@get('controller')}&action=#{@get('action')}"

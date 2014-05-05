define [
  'backbone'
], (
  Backbone
) ->

  class WebServiceActionResponse extends Backbone.Model

  	url: ->
      "#{baseUrl}/api/webservices/response_example?controller=#{@get('controller')}&action=#{@get('action')}"

define [
  'backbone',
  'api-documentation/models/web-service'
], (
  Backbone,
  WebService
) ->

  class WebServices extends Backbone.Collection
    model: WebService

    url: ->
      "#{baseUrl}/api/webservices/list"

    parse: (r) ->
      r.webServices.map (webService) ->
        _.extend webService

    comparator: (item) -> item.get('path')

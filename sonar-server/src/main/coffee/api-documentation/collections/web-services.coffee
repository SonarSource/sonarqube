define [
  'backbone',
  'api-documentation/models/web-service'
], (
  Backbone,
  WebService
) ->

  class WebServices extends Backbone.Collection
    model: WebService

    initialize: ->
      @includeInternals = false

    url: ->
      "#{baseUrl}/api/webservices/list?include_internals=#{@includeInternals}"

    parse: (r) ->
      r.webServices.map (webService) ->
        _.extend webService

    comparator: (item) -> item.get('path')

    toggleInternals: ->
      if @includeInternals
        @includeInternals = false
      else
        @includeInternals = true

      @.fetch()

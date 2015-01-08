define [
  'api-documentation/models/web-service'
], (
  WebService
) ->

  class WebServices extends Backbone.Collection
    model: WebService
    comparator: 'path'

    initialize: ->
      @includeInternals = false

    url: ->
      "#{baseUrl}/api/webservices/list?include_internals=#{@includeInternals}"

    parse: (r) ->
      r.webServices.map (webService) ->
        _.extend webService

    toggleInternals: ->
      if @includeInternals
        @includeInternals = false
      else
        @includeInternals = true

      @fetch()

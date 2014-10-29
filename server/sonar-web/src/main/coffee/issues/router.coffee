define [
  'backbone'
], (
  Backbone
) ->

  class extends Backbone.Router
    routeSeparator: '|'

    routes:
      '': 'emptyQuery'
      ':query': 'index'


    initialize: (options) ->
      @options = options
      @listenTo @options.app.state, 'change:query', @updateRoute


    emptyQuery: ->
      @navigate 'resolved=false', { trigger: true, replace: true }


    index: (query) ->
      filter = @options.app.controller.parseQuery query
      @options.app.state.setQuery filter


    updateRoute: ->
      route = @options.app.controller.getQuery()
      @navigate route


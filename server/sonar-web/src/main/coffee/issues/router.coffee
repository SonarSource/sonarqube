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
      @listenTo @options.app.state, 'change:filter', @updateRoute


    emptyQuery: ->
      @navigate 'resolved=false', { trigger: true, replace: true }


    index: (query) ->
      query = @options.app.controller.parseQuery query
      if query.id?
        filter = @options.app.filters.get query.id
        delete query.id
        filter.fetch().done =>
          if Object.keys(query).length > 0
            @options.app.controller.applyFilter filter, true
            @options.app.state.setQuery query
            @options.app.state.set changed: true
          else
            @options.app.controller.applyFilter filter
      else
        @options.app.state.setQuery query


    updateRoute: ->
      route = @options.app.controller.getRoute()
      @navigate route


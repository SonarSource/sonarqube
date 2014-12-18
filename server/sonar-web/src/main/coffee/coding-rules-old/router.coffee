define [
  'backbone',
], (
  Backbone,
) ->

  class AppRouter extends Backbone.Router

    routes:
      '': 'emptyQuery'
      ':query': 'index'


    initialize: (options) ->
      @app = options.app


    parseQuery: (query, separator) ->
      (query || '').split(separator || '|').map (t) ->
        tokens = t.split('=')
        key: tokens[0], value: decodeURIComponent(tokens[1])


    emptyQuery: ->
      @app.restoreDefaultSorting()
      @index('')


    index: (query) ->
      params = this.parseQuery(query)
      @loadResults(params)


    loadResults: (params) ->
      @app.filterBarView.restoreFromQuery(params)
      if @app.codingRulesFacetsView
        @app.codingRulesFacetsView.restoreFromQuery(params)
      @app.restoreSorting(params)
      @app.fetchFirstPage()

define [
  'backbone'
], (
  Backbone
) ->

  class extends Backbone.Model

    defaults:
      page: 1
      maxResultsReached: false

      query: {}

      facets: ['severities', 'statuses', 'resolutions']


    nextPage: ->
      page = @get 'page'
      @set page: page + 1


    cleanQuery: (query) ->
      q = {}
      Object.keys(query).forEach (key) ->
        q[key] = query[key] if query[key]
      q


    _areQueriesEqual: (a, b) ->
      equal = Object.keys(a).length == Object.keys(b).length
      Object.keys(a).forEach (key) ->
        equal = equal && (a[key] == b[key])
      equal


    updateFilter: (obj) ->
      oldQuery = @get('query')
      query = _.extend {}, oldQuery, obj
      query = @cleanQuery query
      @setQuery query unless @_areQueriesEqual oldQuery, query


    setQuery: (query) ->
      @set { query: query }, { silent: true }
      @set changed: true
      @trigger 'change:query'


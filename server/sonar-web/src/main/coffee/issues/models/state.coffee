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

      facets: ['severities', 'statuses', 'resolutions', 'componentRootUuids']
      allFacets: ['severities', 'statuses', 'resolutions', 'componentRootUuids', 'assignees', 'reporters', 'rule',
                  'languages', 'actionPlan', 'creationDate']


    nextPage: ->
      page = @get 'page'
      @set page: page + 1


    cleanQuery: (query) ->
      q = {}
      Object.keys(query).forEach (key) ->
        q[key] = query[key] if query[key]
      q


    updateFilter: (obj) ->
      filter = @get 'query'
      _.extend filter, obj
      @setQuery @cleanQuery filter


    setQuery: (query) ->
      @set { query: query }, { silent: true }
      @trigger 'change:query'
      @set changed: true


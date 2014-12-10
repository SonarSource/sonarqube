define [
  'backbone.marionette'

  'issues/component-viewer/main'
  'issues/component-viewer/state'
], (
  Marionette

  ComponentViewer
  ComponentViewerState
) ->

  $ = jQuery
  EXTRA_FIELDS = 'actions,transitions,assigneeName,reporterName,actionPlanName'
  PAGE_SIZE = 50
  ALL_FACETS = ['severities', 'statuses', 'resolutions', 'projectUuids', 'componentRootUuids', 'componentUuids', 'assignees', 'reporters', 'rules',
                'tags', 'languages', 'actionPlans', 'creationDate']
  FACET_DATA_FIELDS = ['components', 'projects', 'users', 'rules', 'actionPlans', 'languages']
  FACETS_FROM_SERVER = ['severities', 'statuses', 'resolutions', 'actionPlans', 'projectUuids', 'rules', 'tags'
                        'assignees', 'reporters', 'componentUuids', 'languages']
  TRANSFORM = {
    'resolved': 'resolutions'
    'assigned': 'assignees'
    'planned': 'actionPlans'
    'createdAt': 'creationDate'
    'createdBefore': 'creationDate'
    'createdAfter': 'creationDate'
  }


  class extends Marionette.Controller

    initialize: (options) ->
      @listenTo options.app.state, 'change:query', @fetchIssues


    _issuesParameters: ->
      p: @options.app.state.get 'page'
      ps: PAGE_SIZE
      s: 'FILE_LINE'
      asc: true
      extra_fields: EXTRA_FIELDS
      facets: @_facetsFromServer().join()


    _allFacets: ->
      ALL_FACETS.map (facet) -> { property: facet }


    _enabledFacets: ->
      facets = @options.app.state.get 'facets'
      criteria = Object.keys @options.app.state.get 'query'
      facets = facets.concat criteria
      facets = facets.map (facet) ->
        if TRANSFORM[facet]? then TRANSFORM[facet] else facet
      facets.filter (facet) -> ALL_FACETS.indexOf(facet) != -1


    _facetsFromServer: ->
      facets = @_enabledFacets()
      facets.filter (facet) -> FACETS_FROM_SERVER.indexOf(facet) != -1


    fetchIssues: (firstPage = true) ->
      if firstPage
        @options.app.state.set { selectedIndex: 0, page: 1 }, { silent: true }
        @closeComponentViewer()

      data = @_issuesParameters()
      _.extend data, @options.app.state.get 'query'

      fetchIssuesProcess = window.process.addBackgroundProcess()
      $.get "#{baseUrl}/api/issues/search", data
      .done (r) =>
        issues = @options.app.issues.parseIssues r
        if firstPage
          @options.app.issues.reset issues
        else
          @options.app.issues.add issues
        @options.app.issues.setIndex()
        FACET_DATA_FIELDS.forEach (field) => @options.app.facets[field] = r[field]
        @options.app.facets.reset @_allFacets()
        @options.app.facets.add r.facets, merge: true
        @enableFacets @_enabledFacets()
        @options.app.state.set
          page: r.p
          pageSize: r.ps
          total: r.total
          maxResultsReached: r.p * r.ps >= r.total
        window.process.finishBackgroundProcess fetchIssuesProcess
      .fail ->
        window.process.failBackgroundProcess fetchIssuesProcess


    fetchNextPage: ->
      @options.app.state.nextPage()
      @fetchIssues false


    fetchFilters: ->
      $.get "#{baseUrl}/api/issue_filters/app", (r) =>
        @options.app.state.set
          canBulkChange: r.canBulkChange
          canManageFilters: r.canManageFilters
        @options.app.filters.reset r.favorites


    enableFacet: (id) ->
      facet = @options.app.facets.get id
      if facet.has('values') || FACETS_FROM_SERVER.indexOf(id) == -1
        facet.set enabled: true
      else
        p = window.process.addBackgroundProcess()
        @requestFacet(id)
        .done =>
          facet.set enabled: true
          window.process.finishBackgroundProcess p
        .fail ->
          window.process.failBackgroundProcess p


    disableFacet: (id) ->
      facet = @options.app.facets.get id
      facet.set enabled: false
      @options.app.facetsView.children.findByModel(facet).disable()


    toggleFacet: (id) ->
      facet = @options.app.facets.get id
      if facet.get('enabled') then @disableFacet(id) else @enableFacet(id)


    enableFacets: (facets) ->
      facets.forEach @enableFacet, @


    _mergeCollections: (a, b) ->
      collection = new Backbone.Collection a
      collection.add b, merge: true
      collection.toJSON()


    requestFacet: (id) ->
      facet = @options.app.facets.get id
      data = _.extend { facets: id, ps: 1 }, @options.app.state.get('query')
      $.get "#{baseUrl}/api/issues/search", data, (r) =>
        FACET_DATA_FIELDS.forEach (field) =>
          @options.app.facets[field] = @_mergeCollections @options.app.facets[field], r[field]
        facetData = _.findWhere r.facets, property: id
        facet.set facetData if facetData?


    newSearch: ->
      @options.app.state.unset 'filter'
      @options.app.state.setQuery resolved: 'false'


    applyFilter: (filter, ignoreQuery = false) ->
      unless ignoreQuery
        filterQuery = @parseQuery filter.get 'query'
        @options.app.state.setQuery filterQuery
      @options.app.state.set filter: filter, changed: false


    parseQuery: (query, separator = '|') ->
      q = {}
      (query || '').split(separator).forEach (t) ->
        tokens = t.split('=')
        if tokens[0] && tokens[1]?
          q[tokens[0]] = decodeURIComponent tokens[1]
      q


    getQuery: (separator = '|') ->
      filter = @options.app.state.get 'query'
      route = []
      _.map filter, (value, property) ->
        route.push "#{property}=#{encodeURIComponent value}"
      route.join separator


    getRoute: (separator = '|') ->
      filter = @options.app.state.get 'filter'
      query = @getQuery separator
      if filter?
        if @options.app.state.get('changed') && query.length > 0
          query = "id=#{filter.id}|#{query}"
        else
          query = "id=#{filter.id}"
      query


    _prepareComponent: (issue) ->
      key: issue.get 'component'
      name: issue.get 'componentLongName'
      qualifier: issue.get 'componentQualifier'
      project: issue.get 'project'
      projectName: issue.get 'projectLongName'


    showComponentViewer: (issue) ->
      @options.app.layout.workspaceComponentViewerRegion.reset()
      key.setScope 'componentViewer'
      @options.app.issuesView.unbindScrollEvents()
      @options.app.state.set 'component', @_prepareComponent(issue)
      @options.app.componentViewer = new ComponentViewer app: @options.app
      @options.app.layout.workspaceComponentViewerRegion.show @options.app.componentViewer
      @options.app.layout.showComponentViewer()
      @options.app.componentViewer.openFileByIssue issue


    closeComponentViewer: ->
      key.setScope 'list'
      # close all popups
      $('body').click()
      @options.app.state.unset 'component'
      @options.app.layout.workspaceComponentViewerRegion.reset()
      @options.app.layout.hideComponentViewer()
      @options.app.issuesView.bindScrollEvents()
      @options.app.issuesView.scrollToIssue()


    selectNextIssue: ->
      index = @options.app.state.get('selectedIndex') + 1
      if index < @options.app.issues.length
        @options.app.state.set selectedIndex: index
      else
        unless @options.app.state.get('maxResultsReached')
          @fetchNextPage().done =>
            @options.app.state.set selectedIndex: index


    selectPreviousIssue: ->
      index = @options.app.state.get('selectedIndex') - 1
      if index >= 0
        @options.app.state.set selectedIndex: index

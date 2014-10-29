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


  class extends Marionette.Controller

    initialize: (options) ->
      @listenTo options.app.state, 'change:query', @fetchIssues


    _issuesParameters: ->
      p: @options.app.state.get 'page'
      ps: PAGE_SIZE
      s: 'FILE_LINE'
      asc: true
      extra_fields: EXTRA_FIELDS
      facets: @options.app.state.get('facets').join()


    _allFacets: ->
      @options.app.state.get('allFacets').map (facet) -> { property: facet }


    fetchIssues: (firstPage = true) ->
      if firstPage
        @options.app.state.set { selectedIndex: 0, page: 1 }, { silent: true }

      data = @_issuesParameters()
      _.extend data, @options.app.state.get 'query'

      fetchIssuesProcess = window.process.addBackgroundProcess()
      $.get "#{baseUrl}/api/issues/search", data, (r) =>
        issues = @options.app.issues.parseIssues r
        if firstPage
          @options.app.issues.reset issues
        else
          @options.app.issues.add issues

        _.extend @options.app.facets,
          components: r.components
          projects: r.projects
          rules: r.rules
          users: r.users
          actionPlans: r.actionPlans
        @options.app.facets.reset @_allFacets()
        @options.app.facets.add r.facets, merge: true
        @enableFacets @options.app.state.get 'facets'

        @options.app.state.set
          page: r.p
          pageSize: r.ps
          total: r.total
          maxResultsReached: r.p * r.ps >= r.total

        window.process.finishBackgroundProcess fetchIssuesProcess


    fetchNextPage: ->
      @options.app.state.nextPage()
      @fetchIssues false


    fetchFilters: ->
      $.get "#{baseUrl}/api/issue_filters/app", (r) =>
        @options.app.state.set
          canBulkChange: r.canBulkChange
          canManageFilters: r.canManageFilters
        @options.app.filters.reset r.favorites


    enableFacet: (facet) ->
      @options.app.facets.get(facet).set enabled: true


    enableFacets: (facets) ->
      facets.forEach @enableFacet, @


    newSearch: ->
      @options.app.state.unset 'filter'
      @options.app.state.setQuery resolved: 'false'


    applyFilter: (filter) ->
      query = @parseQuery filter.get 'query'
      @options.app.state.setQuery query
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
        route.push "#{property}=#{decodeURIComponent value}"
      route.join separator


    _prepareComponent: (issue) ->
      key: issue.get 'component'
      name: issue.get 'componentLongName'
      qualifier: issue.get 'componentQualifier'
      project: issue.get 'project'
      projectName: issue.get 'projectLongName'


    showComponentViewer: (issue) ->
      key.setScope 'componentViewer'
      @options.app.issuesView.unbindScrollEvents()
      @options.app.state.set 'component', @_prepareComponent(issue)
      @options.app.componentViewer = new ComponentViewer
        app: @options.app
        model: new ComponentViewerState()
      @options.app.layout.workspaceComponentViewerRegion.show @options.app.componentViewer
      @options.app.layout.showComponentViewer()
      @options.app.componentViewer.openFileByIssue issue


    closeComponentViewer: ->
      key.setScope 'list'
      @options.app.state.unset 'component'
      @options.app.componentViewer.unbindScrollEvents()
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

define [
  'components/navigator/controller'

  'issues/component-viewer/main'
  'issues/component-viewer/state'
], (
  Controller

  ComponentViewer
  ComponentViewerState
) ->

  $ = jQuery
  EXTRA_FIELDS = 'actions,transitions,assigneeName,reporterName,actionPlanName'
  FACET_DATA_FIELDS = ['components', 'projects', 'users', 'rules', 'actionPlans', 'languages']


  class extends Controller
    allFacets: ['severities', 'statuses', 'resolutions', 'projectUuids', 'moduleUuids', 'componentUuids', 'assignees',
                'reporters', 'rules', 'tags', 'languages', 'actionPlans', 'creationDate', 'issues'],
    facetsFromServer: ['severities', 'statuses', 'resolutions', 'actionPlans', 'projectUuids', 'rules', 'tags',
                       'assignees', 'reporters', 'componentUuids', 'languages'],
    transform: {
      'resolved': 'resolutions'
      'assigned': 'assignees'
      'planned': 'actionPlans'
      'createdAt': 'creationDate'
      'createdBefore': 'creationDate'
      'createdAfter': 'creationDate'
    },

    _issuesParameters: ->
      p: @options.app.state.get 'page'
      ps: @pageSize
      s: 'FILE_LINE'
      asc: true
      extra_fields: EXTRA_FIELDS
      facets: @_facetsFromServer().join()


    fetchList: (firstPage = true) ->
      if firstPage
        @options.app.state.set { selectedIndex: 0, page: 1 }, { silent: true }
        @closeComponentViewer()

      data = @_issuesParameters()
      _.extend data, @options.app.state.get 'query'

      fetchIssuesProcess = window.process.addBackgroundProcess()
      $.get "#{baseUrl}/api/issues/search", data
      .done (r) =>
        issues = @options.app.list.parseIssues r
        if firstPage
          @options.app.list.reset issues
        else
          @options.app.list.add issues
        @options.app.list.setIndex()
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


    fetchFilters: ->
      $.get "#{baseUrl}/api/issue_filters/app", (r) =>
        @options.app.state.set
          canBulkChange: r.canBulkChange
          canManageFilters: r.canManageFilters
        @options.app.filters.reset r.favorites


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


    parseQuery: ->
      q = super
      # Do not allow to modify the sorting
      delete q.asc
      delete q.s
      q


    getRoute: ->
      filter = @options.app.state.get 'filter'
      query = super
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
      @options.app.issuesView.scrollTo()


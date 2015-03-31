#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

define [
  'components/navigator/controller'

  'issues/component-viewer/main'
  'issues/workspace-home-view'
], (
  Controller

  ComponentViewer
  HomeView
) ->

  $ = jQuery
  EXTRA_FIELDS = 'actions,transitions,assigneeName,reporterName,actionPlanName'
  FACET_DATA_FIELDS = ['components', 'projects', 'users', 'rules', 'actionPlans', 'languages']


  class extends Controller

    _facetsFromServer: ->
      facets = super || []
      facets.push 'assigned_to_me'
      facets


    _issuesParameters: ->
      p: @options.app.state.get 'page'
      ps: @pageSize
      s: 'FILE_LINE'
      asc: true
      extra_fields: EXTRA_FIELDS
      facets: @_facetsFromServer().join()


    _myIssuesFromResponse: (r) ->
      myIssuesData = _.findWhere r.facets, property: 'assigned_to_me'
      if myIssuesData? && _.isArray(myIssuesData.values) && myIssuesData.values.length > 0
        @options.app.state.set { myIssues: myIssuesData.values[0].count }, { silent: true }
      else
        @options.app.state.unset 'myIssues', { silent: true }


    fetchList: (firstPage = true) ->
      if firstPage
        @options.app.state.set { selectedIndex: 0, page: 1 }, { silent: true }
        @hideHomePage()
        @closeComponentViewer()

      data = @_issuesParameters()
      _.extend data, @options.app.state.get 'query'
      _.extend data, @options.app.state.get 'contextQuery' if @options.app.state.get 'isContext'

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
        @options.app.facets.add _.reject(r.facets, (f) -> f.property == 'assigned_to_me'), merge: true
        @_myIssuesFromResponse r
        @enableFacets @_enabledFacets()
        @options.app.state.set
          page: r.p
          pageSize: r.ps
          total: r.total
          maxResultsReached: r.p * r.ps >= r.total
        if firstPage && @isIssuePermalink()
          @showComponentViewer @options.app.list.first()


    isIssuePermalink: ->
      query = @options.app.state.get('query')
      query.issues? && @options.app.list.length == 1


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
      return @requestAssigneeFacet() if id == 'assignees'
      facet = @options.app.facets.get id
      data = _.extend { facets: id, ps: 1 }, @options.app.state.get('query')
      _.extend data, @options.app.state.get 'contextQuery' if @options.app.state.get 'isContext'
      $.get "#{baseUrl}/api/issues/search", data, (r) =>
        FACET_DATA_FIELDS.forEach (field) =>
          @options.app.facets[field] = @_mergeCollections @options.app.facets[field], r[field]
        facetData = _.findWhere r.facets, property: id
        facet.set facetData if facetData?


    requestAssigneeFacet: ->
      facet = @options.app.facets.get 'assignees'
      data = _.extend { facets: 'assignees,assigned_to_me', ps: 1 }, @options.app.state.get('query')
      _.extend data, @options.app.state.get 'contextQuery' if @options.app.state.get 'isContext'
      $.get "#{baseUrl}/api/issues/search", data, (r) =>
        FACET_DATA_FIELDS.forEach (field) =>
          @options.app.facets[field] = @_mergeCollections @options.app.facets[field], r[field]
        facetData = _.findWhere r.facets, property: 'assignees'
        @_myIssuesFromResponse r
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


    getQuery: (separator = '|', addContext = false) ->
      filter = @options.app.state.get 'query'
      _.extend filter, @options.app.state.get 'contextQuery' if addContext && @options.app.state.get 'isContext'
      route = []
      _.map filter, (value, property) ->
        route.push '' + property + '=' + encodeURIComponent(value)
      route.join separator


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


    showHomePage: ->
      @fetchList()
      @options.app.layout.workspaceComponentViewerRegion.reset()
      key.setScope 'home'
      @options.app.issuesView.unbindScrollEvents()
      @options.app.homeView = new HomeView app: @options.app
      @options.app.layout.workspaceHomeRegion.show @options.app.homeView
      @options.app.layout.showHomePage()


    hideHomePage: ->
      @options.app.layout.workspaceComponentViewerRegion.reset()
      @options.app.layout.workspaceHomeRegion.reset()
      key.setScope 'list'
      @options.app.layout.hideHomePage()
      @options.app.issuesView.bindScrollEvents()
      @options.app.issuesView.scrollTo()


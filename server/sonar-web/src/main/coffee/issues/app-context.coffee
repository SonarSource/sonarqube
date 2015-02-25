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

requirejs.config
  baseUrl: "#{baseUrl}/js"


requirejs [
  'issues/models/state'
  'issues/layout'
  'issues/models/issues'
  'components/navigator/models/facets'
  'issues/models/filters'

  'issues/controller'
  'issues/router'

  'issues/workspace-list-view'
  'issues/workspace-header-view'

  'issues/facets-view'
  'issues/filters-view'
], (
  State
  Layout
  Issues
  Facets
  Filters

  Controller
  Router

  WorkspaceListView
  WorkspaceHeaderView

  FacetsView
  FiltersView
) ->

  $ = jQuery
  App = new Marionette.Application


  App.getContextQuery = ->
    componentUuids: window.config.resource


  App.getRestrictedFacets = ->
    'TRK': ['projectUuids']
    'BRC': ['projectUuids']
    'DIR': ['projectUuids', 'moduleUuids', 'directories']
    'DEV': ['authors']


  App.updateContextFacets = ->
    facets = @state.get 'facets'
    allFacets = @state.get 'allFacets'
    facetsFromServer = @state.get 'facetsFromServer'
    @state.set
      facets: facets
      allFacets: _.difference allFacets, @getRestrictedFacets()[window.config.resourceQualifier]
      facetsFromServer: _.difference facetsFromServer, @getRestrictedFacets()[window.config.resourceQualifier]


  App.addInitializer ->
    @state = new State
      isContext: true,
      contextQuery: @getContextQuery()
      contextComponentUuid: window.config.resource
      contextComponentName: window.config.resourceName
      contextComponentQualifier: window.config.resourceQualifier
    @updateContextFacets()
    @list = new Issues()
    @facets = new Facets()
    @filters = new Filters()


  App.addInitializer ->
    @layout = new Layout app: @
    $('.issues').empty().append @layout.render().el
    $('#footer').addClass('search-navigator-footer');


  App.addInitializer ->
    @controller = new Controller app: @


  App.addInitializer ->
    @issuesView = new WorkspaceListView
      app: @
      collection: @list
    @layout.workspaceListRegion.show @issuesView
    @issuesView.bindScrollEvents()


  App.addInitializer ->
    @workspaceHeaderView = new WorkspaceHeaderView
      app: @
      collection: @list
    @layout.workspaceHeaderRegion.show @workspaceHeaderView


  App.addInitializer ->
    @facetsView = new FacetsView
      app: @
      collection: @facets
    @layout.facetsRegion.show @facetsView


  App.addInitializer ->
    @controller.fetchFilters().done =>
      key.setScope 'list'
      @router = new Router app: @
      Backbone.history.start()


  l10nXHR = window.requestMessages()
  jQuery.when(l10nXHR).done -> App.start()

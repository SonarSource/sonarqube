requirejs.config
  baseUrl: "#{baseUrl}/js"

  paths:
    'backbone': 'third-party/backbone'
    'backbone.marionette': 'third-party/backbone.marionette'
    'handlebars': 'third-party/handlebars'

  shim:
    'backbone.marionette':
      deps: ['backbone']
      exports: 'Marionette'
    'backbone':
      exports: 'Backbone'
    'handlebars':
      exports: 'Handlebars'


requirejs [
  'backbone', 'backbone.marionette'

  'issues/models/state'
  'issues/layout'
  'issues/models/issues'
  'issues/models/facets'
  'issues/models/filters'

  'issues/controller'
  'issues/router'

  'issues/workspace-list-view'
  'issues/workspace-header-view'

  'issues/facets-view'
  'issues/filters-view'

  'common/handlebars-extensions'
], (
  Backbone, Marionette

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


  App.addInitializer ->
    @layout = new Layout()
    $('.issues').empty().append @layout.render().el


  App.addInitializer ->
    @state = new State()
    @issues = new Issues()
    @facets = new Facets()
    @filters = new Filters()


  App.addInitializer ->
    @controller = new Controller app: @


  App.addInitializer ->
    @controller.fetchFilters()


  App.addInitializer ->
    @issuesView = new WorkspaceListView
      app: @
      collection: @issues
    @layout.workspaceListRegion.show @issuesView
    @issuesView.bindScrollEvents()


  App.addInitializer ->
    @workspaceHeaderView = new WorkspaceHeaderView
      app: @
      collection: @issues
    @layout.workspaceHeaderRegion.show @workspaceHeaderView


  App.addInitializer ->
    @facetsView = new FacetsView
      app: @
      collection: @facets
    @layout.facetsRegion.show @facetsView


  App.addInitializer ->
    @filtersView = new FiltersView
      app: @
      collection: @filters
    @layout.filtersRegion.show @filtersView


  App.addInitializer ->
    key.setScope 'list'
    @router = new Router app: @
    Backbone.history.start()


  l10nXHR = window.requestMessages()
  jQuery.when(l10nXHR).done -> App.start()

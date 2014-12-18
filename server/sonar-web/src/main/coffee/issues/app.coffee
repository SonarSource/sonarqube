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
  'components/navigator/models/facets'
  'issues/models/filters'

  'issues/controller'
  'issues/router'

  'issues/workspace-list-view'
  'issues/workspace-header-view'

  'issues/facets-view'
  'issues/filters-view'

  'issues/help-view'

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

  HelpView
) ->

  $ = jQuery
  App = new Marionette.Application
  issuesAppProcess = window.process.addBackgroundProcess()


  App.addInitializer ->
    @layout = new Layout()
    $('.issues').empty().append @layout.render().el


  App.addInitializer ->
    @state = new State()
    @list = new Issues()
    @facets = new Facets()
    @filters = new Filters()


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
    @filtersView = new FiltersView
      app: @
      collection: @filters
    @layout.filtersRegion.show @filtersView


  App.addInitializer ->
    $(window).on 'keypress', (e) =>
      tagName = e.target.tagName
      unless tagName == 'INPUT' || tagName == 'SELECT' || tagName == 'TEXTAREA'
        code = e.keyCode || e.which
        if code == 63
          @helpView = new HelpView app: @
          @helpView.render()


  App.addInitializer ->
    @controller.fetchFilters().done =>
      key.setScope 'list'
      @router = new Router app: @
      Backbone.history.start()
      window.process.finishBackgroundProcess issuesAppProcess


  l10nXHR = window.requestMessages()
  jQuery.when(l10nXHR).done -> App.start()

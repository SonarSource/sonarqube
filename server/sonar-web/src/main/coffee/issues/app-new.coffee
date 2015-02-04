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

  App.addInitializer ->
    @state = new State()
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
    @filtersView = new FiltersView
      app: @
      collection: @filters
    @layout.filtersRegion.show @filtersView


  App.addInitializer ->
    @controller.fetchFilters().done =>
      key.setScope 'list'
      @router = new Router app: @
      Backbone.history.start()


  l10nXHR = window.requestMessages()
  jQuery.when(l10nXHR).done -> App.start()

requirejs.config

  paths:
    'backbone': '../third-party/backbone'
    'backbone.marionette': '../third-party/backbone.marionette'
    'handlebars': '../third-party/handlebars'
    'moment': '../third-party/moment'
    'select-list': '../select-list'

  shim:
    'backbone.marionette':
      deps: ['backbone']
      exports: 'Marionette'
    'backbone':
      exports: 'Backbone'
    'handlebars':
      exports: 'Handlebars'
    'moment':
      exports: 'moment'
    'select-list':
      exports: 'SelectList'


requirejs [
  'backbone', 'backbone.marionette', 'handlebars',
  'collections/quality-gates',
  'collections/metrics',
  'views/quality-gate-sidebar-list-view',
  'router'
  '../handlebars-extensions'
], (
  Backbone, Marionette, Handlebars,
  QualityGates,
  Metrics,
  QualityGateSidebarListItemView,
  QualityGateRouter
) ->

  # Create a Quality Gate Application
  App = new Marionette.Application

  App.metrics = new Metrics
  App.qualityGates = new QualityGates

  App.openFirstQualityGate = ->
    if @qualityGates.length > 0
      @router.navigate "show/#{@qualityGates.models[0].get('id')}", trigger: true
    else
      App.contentRegion.reset()

  App.deleteQualityGate = (id) ->
    App.qualityGates.remove id
    App.openFirstQualityGate()

  App.unsetDefaults = (id) ->
    App.qualityGates.each (gate) ->
      gate.set('default', false) unless gate.id == id

  # Define page regions
  App.addRegions
    sidebarRegion: '#sidebar'
    contentRegion: '#content'

  # Construct sidebar
  App.addInitializer ->
    @qualityGateSidebarListView = new QualityGateSidebarListItemView
      collection: @qualityGates
      app: @
    @sidebarRegion.show @qualityGateSidebarListView

  # Start router
  App.addInitializer ->
    @router = new QualityGateRouter app: @
    Backbone.history.start()

  # Open first quality gate when come to the page
  App.addInitializer ->
    initial = Backbone.history.fragment == ''
    App.openFirstQualityGate() if initial

  # Load metrics and the list of quality gates before start the application
  jQuery.when(App.metrics.fetch(), App.qualityGates.fetch()).done ->
    # Start the application
    App.start()

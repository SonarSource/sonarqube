requirejs.config
  baseUrl: "#{baseUrl}/javascripts"

  paths:
    'backbone': 'third-party/backbone'
    'backbone.marionette': 'third-party/backbone.marionette'
    'handlebars': 'third-party/handlebars'
    'moment': 'third-party/moment'
    'select-list': 'common/select-list'

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
  'quality-gate/collections/quality-gates',
  'quality-gate/collections/metrics',
  'quality-gate/views/quality-gate-sidebar-list-view',
  'quality-gate/views/quality-gate-actions-view',
  'quality-gate/views/quality-gate-edit-view',
  'quality-gate/router'
  'common/handlebars-extensions'
], (
  Backbone, Marionette, Handlebars,
  QualityGates,
  Metrics,
  QualityGateSidebarListItemView,
  QualityGateActionsView,
  QualityGateEditView,
  QualityGateRouter
) ->

  # Create a generic error handler for ajax requests
  jQuery.ajaxSetup
    error: (jqXHR) ->
      if jqXHR.responseJSON?.errors?
        alert _.pluck(jqXHR.responseJSON.errors, 'msg').join '. '
      else
        alert jqXHR.responseText

  # Add html class to mark the page as navigator page
  jQuery('html').addClass('navigator-page quality-gates-page');

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
    headerRegion: '.navigator-header'
    actionsRegion: '.navigator-actions'
    listRegion: '.navigator-results'
    detailsRegion: '.navigator-details'

  # Construct actions bar
  App.addInitializer ->
    @qualityGateActionsView = new QualityGateActionsView
      app: @
    @actionsRegion.show @qualityGateActionsView

  # Construct sidebar
  App.addInitializer ->
    @qualityGateSidebarListView = new QualityGateSidebarListItemView
      collection: @qualityGates
      app: @
    @listRegion.show @qualityGateSidebarListView

  # Construct edit view
  App.addInitializer ->
    @qualityGateEditView = new QualityGateEditView app: @
    @qualityGateEditView.render()

  # Start router
  App.addInitializer ->
    @router = new QualityGateRouter app: @
    Backbone.history.start()

  # Open first quality gate when come to the page
  App.addInitializer ->
    initial = Backbone.history.fragment == ''
    App.openFirstQualityGate() if initial

  # Load metrics and the list of quality gates before start the application
  jQuery.when(App.metrics.fetch(), App.qualityGates.fetch())
    .done ->
      jQuery('.quality-gate-page-loader').remove()
      # Start the application
      App.start()

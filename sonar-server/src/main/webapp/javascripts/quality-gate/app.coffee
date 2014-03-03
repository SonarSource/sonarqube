requirejs.config
  baseUrl: "#{baseUrl}/javascripts"

  paths:
    'jquery': 'third-party/jquery'
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
  'quality-gate/router',
  'quality-gate/layout',
  'common/handlebars-extensions'
], (
  Backbone, Marionette, Handlebars,
  QualityGates,
  Metrics,
  QualityGateSidebarListItemView,
  QualityGateActionsView,
  QualityGateEditView,
  QualityGateRouter,
  QualityGateLayout
) ->

  # Create a generic error handler for ajax requests
  jQuery.ajaxSetup
    error: (jqXHR) ->
      text = jqXHR.responseText
      errorBox = jQuery('.modal-error')
      if jqXHR.responseJSON?.errors?
        text = _.pluck(jqXHR.responseJSON.errors, 'msg').join '. '
      if errorBox.length > 0
        errorBox.show().text text
      else
        alert text


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
      App.layout.detailsRegion.reset()


  App.deleteQualityGate = (id) ->
    App.qualityGates.remove id
    App.openFirstQualityGate()


  App.unsetDefaults = (id) ->
    App.qualityGates.each (gate) ->
      gate.set('default', false) unless gate.id == id


  # Construct layout
  App.addInitializer ->
    @layout = new QualityGateLayout app: @
    jQuery('body').append @layout.render().el


  # Construct actions bar
  App.addInitializer ->
    @qualityGateActionsView = new QualityGateActionsView
      app: @
    @layout.actionsRegion.show @qualityGateActionsView


  # Construct sidebar
  App.addInitializer ->
    @qualityGateSidebarListView = new QualityGateSidebarListItemView
      collection: @qualityGates
      app: @
    @layout.listRegion.show @qualityGateSidebarListView


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
  qualityGatesXHR = App.qualityGates.fetch()
  jQuery.when(App.metrics.fetch(), qualityGatesXHR)
    .done ->
      # Set permissions
      App.canEdit = qualityGatesXHR.responseJSON.edit

      # Remove the initial spinner
      jQuery('.quality-gate-page-loader').remove()

      # Start the application
      App.start()

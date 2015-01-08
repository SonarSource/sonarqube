requirejs.config
  baseUrl: "#{baseUrl}/js"


requirejs [
  'quality-gate/collections/quality-gates',
  'quality-gate/views/quality-gate-sidebar-list-view',
  'quality-gate/views/quality-gate-actions-view',
  'quality-gate/views/quality-gate-edit-view',
  'quality-gate/router',
  'quality-gate/layout'
], (
  QualityGates,
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
      else
        text = t 'default_error_message'
      if errorBox.length > 0
        errorBox.show().text text
      else
        alert text


  # Add html class to mark the page as navigator page
  jQuery('html').addClass('navigator-page quality-gates-page');


  # Create a Quality Gate Application
  App = new Marionette.Application


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
    jQuery('#content').append @layout.render().el
    @layout.onResize()


  # Construct actions bar
  App.addInitializer ->
    @codingRulesHeaderView = new QualityGateActionsView
      app: @
    @layout.actionsRegion.show @codingRulesHeaderView


  # Construct sidebar
  App.addInitializer ->
    @qualityGateSidebarListView = new QualityGateSidebarListItemView
      collection: @qualityGates
      app: @
    @layout.resultsRegion.show @qualityGateSidebarListView


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


  # Call app, Load metrics and the list of quality gates before start the application
  appXHR = jQuery.ajax
    url: "#{baseUrl}/api/qualitygates/app"
  .done (r) =>
      App.canEdit = r.edit
      App.periods = r.periods
      App.metrics = r.metrics

  qualityGatesXHR = App.qualityGates.fetch()

  # Message bundles
  l10nXHR = window.requestMessages()

  jQuery.when(qualityGatesXHR, appXHR, l10nXHR)
    .done ->
      # Remove the initial spinner
      jQuery('#quality-gate-page-loader').remove()

      # Start the application
      App.start()

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

  'monitoring/layout'
  'monitoring/models/reports'
  'monitoring/views/reports-view'
  'monitoring/views/header-view'

  'common/handlebars-extensions'
  'monitoring/mockjax'
], (
  Backbone, Marionette
  MonitoringLayout
  Reports
  ReportsView
  HeaderView
) ->

  # Add html class to mark the page as navigator page
  jQuery('html').addClass 'navigator-page'


  # Create an Application
  App = new Marionette.Application


  # Construct layout
  App.addInitializer ->
    @layout = new MonitoringLayout app: @
    jQuery('#monitoring').empty().append @layout.render().el
    @layout.onResize()


  App.addInitializer ->
    @headerView = new HeaderView app: @
    @layout.headerRegion.show @headerView


  App.addInitializer ->
    @reports = new Reports()

    @reportsView = new ReportsView
      app: @
      collection: @reports
    @layout.resultsRegion.show @reportsView

    @reports.fetch()


#  App.addInitializer ->
#    @codingRulesActionsView = new CodingRulesActionsView
#      app: @
#      collection: @reports
#    @layout.actionsRegion.show @codingRulesActionsView


  # Message bundles
  l10nXHR = window.requestMessages()


  jQuery.when(l10nXHR).done -> App.start()

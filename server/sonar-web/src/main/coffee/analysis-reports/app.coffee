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

  'analysis-reports/layout'
  'analysis-reports/models/reports'
  'analysis-reports/views/reports-view'
  'analysis-reports/views/header-view'
  'analysis-reports/views/actions-view'

  'common/handlebars-extensions'
  'analysis-reports/mockjax'
], (
  Backbone, Marionette
  Layout
  Reports
  ReportsView
  HeaderView
  ActionsView
) ->

  # Add html class to mark the page as navigator page
  jQuery('html').addClass 'navigator-page'


  # Create an Application
  App = new Marionette.Application


  App.addInitializer ->
    @state = new Backbone.Model active: true
    @state.on 'change:active', => @reports?.fetch()


  App.addInitializer ->
    @layout = new Layout app: @
    jQuery('#analysis-reports').empty().append @layout.render().el
    @layout.onResize()


  App.addInitializer ->
    @reports = new Reports()

    @reportsView = new ReportsView
      app: @
      collection: @reports
    @layout.resultsRegion.show @reportsView

    @reports.fetch()


  App.addInitializer ->
    @headerView = new HeaderView app: @
    @layout.headerRegion.show @headerView

    @actionsView = new ActionsView
      app: @
      collection: @reports
    @layout.actionsRegion.show @actionsView



  l10nXHR = window.requestMessages()
  jQuery.when(l10nXHR).done -> App.start()

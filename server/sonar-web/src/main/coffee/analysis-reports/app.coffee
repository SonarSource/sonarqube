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

  'analysis-reports/router'
  'analysis-reports/layout'
  'analysis-reports/models/reports'
  'analysis-reports/views/reports-view'
  'analysis-reports/views/header-view'
  'analysis-reports/views/actions-view'

  'common/handlebars-extensions'
], (
  Backbone, Marionette
  Router
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


  App.fetchReports = ->
    process = window.process.addBackgroundProcess()
    fetch = if @state.get 'active' then @reports.fetchActive() else @reports.fetchHistory()
    @layout.showSpinner 'actionsRegion'
    @layout.resultsRegion.reset()
    fetch.done =>
      @state.set page: @reports.paging.page
      @reportsView = new ReportsView
        app: @
        collection: @reports
      @layout.resultsRegion.show @reportsView

      unless @state.get('active') || @reports.paging.maxResultsReached
        @reportsView.bindScrollEvents() unless @state.get 'active'

      @actionsView = new ActionsView
        app: @
        collection: @reports
      @layout.actionsRegion.show @actionsView

      @layout.onResize()

      window.process.finishBackgroundProcess process


  App.fetchNextPage = ->
    process = window.process.addBackgroundProcess()
    @reports.fetchHistory
      data:
        p: @state.get('page') + 1
      remove: false
    .done =>
      @state.set page: @reports.paging.page
      window.process.finishBackgroundProcess process


  App.addInitializer ->
    @state = new Backbone.Model()
    @state.on 'change:active', => @fetchReports()


  App.addInitializer ->
    @layout = new Layout app: @
    jQuery('#analysis-reports').empty().append @layout.render().el


  App.addInitializer ->
    @headerView = new HeaderView app: @
    @layout.headerRegion.show @headerView


  App.addInitializer ->
    @reports = new Reports()
    @router = new Router app: @
    Backbone.history.start()


  l10nXHR = window.requestMessages()
  jQuery.when(l10nXHR).done -> App.start()

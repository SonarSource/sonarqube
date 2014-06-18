requirejs.config
  baseUrl: "#{baseUrl}/js"

  paths:
    'backbone': 'third-party/backbone'
    'backbone.marionette': 'third-party/backbone.marionette'
    'handlebars': 'third-party/handlebars'
    'jquery.mockjax': 'third-party/jquery.mockjax'

  shim:
    'backbone.marionette':
      deps: ['backbone']
      exports: 'Marionette'
    'backbone':
      exports: 'Backbone'
    'handlebars':
      exports: 'Handlebars'


requirejs [
  'backbone.marionette'
  'component-viewer/main'
  'drilldown/conf'
], (
  Marionette
  ComponentViewer
  MetricConf
) ->

  $ = jQuery

  App = new Marionette.Application()


  App.addRegions
    viewerRegion: '#accordion-panel'


  App.requestComponentViewer = ->
    unless App.componentViewer?
      App.componentViewer = new ComponentViewer()
      App.viewerRegion.show App.componentViewer
    App.componentViewer



  App.addInitializer ->
    viewer = App.requestComponentViewer()
    if window.metric?
      metricConf = MetricConf[window.metric]
      if metricConf?
        activeHeaderTab = metricConf.tab
        activeHeaderItem = metricConf.item
    viewer.open(window.fileKey).done ->
      if activeHeaderTab? && activeHeaderItem?
        viewer.state.set activeHeaderTab: activeHeaderTab, activeHeaderItem: activeHeaderItem
        viewer.headerView.render()
      else
        viewer.showAllLines()


  # Message bundles
  l10nXHR = window.requestMessages()


  $.when(l10nXHR).done ->
    # Start the application
    App.start()

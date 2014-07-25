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
  el = $('#accordion-panel')


  App.addRegions
    viewerRegion: '#accordion-panel'


  App.resizeContainer = ->
    width = $(window).width()
    height = $(window).height() - el.offset().top - $('#footer').height() - 10
    el.innerWidth(width).innerHeight(height)


  App.requestComponentViewer = ->
    unless App.componentViewer?
      @resizeContainer()
      $(window).on 'resize', => @resizeContainer()
      App.componentViewer = new ComponentViewer
        elementToFit: el
      App.viewerRegion.show App.componentViewer
    App.componentViewer



  App.addInitializer ->
    viewer = App.requestComponentViewer()
    if window.metric?
      metricConf = MetricConf[window.metric]
      if metricConf?
        activeHeaderTab = metricConf.tab
        activeHeaderItem = metricConf.item
    viewer.open window.fileKey
    viewer.on 'loaded', ->
      viewer.off 'loaded'
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

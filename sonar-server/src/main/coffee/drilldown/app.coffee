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
    # Define parameters
    drilldown = window.drilldown || {}
    activeHeaderTab = 'issues'
    activeHeaderItem = '.js-filter-unresolved-issues'
    if drilldown.metric?
      metricConf = MetricConf[drilldown.metric]
      if metricConf?
        activeHeaderTab = metricConf.tab
        activeHeaderItem = metricConf.item
      else
        activeHeaderTab = 'basic'
        activeHeaderItem = null
    else if drilldown.rule?
      activeHeaderTab = 'issues'
      activeHeaderItem = ".js-filter-rule[data-rule='#{drilldown.rule}']"
    else if drilldown.severity?
      activeHeaderTab = 'issues'
      activeHeaderItem = ".js-filter-#{drilldown.severity}-issues"

    # Add event listeners
    $('.js-drilldown-link').on 'click', (e) ->
      e.preventDefault()
      key = $(e.currentTarget).data 'key'
      viewer = App.requestComponentViewer()
      viewer.open(key).done ->
        if activeHeaderTab? && activeHeaderItem?
          viewer.state.set activeHeaderTab: activeHeaderTab, activeHeaderItem: activeHeaderItem
          viewer.render()
        else if activeHeaderTab?
          viewer.state.set activeHeaderTab: activeHeaderTab
          viewer.headerView.render()
          viewer.showAllLines()
        else if drilldown.period?
          viewer.enablePeriod drilldown.period, 'issues'
        else
          viewer.showAllLines()


  # Message bundles
  l10nXHR = window.requestMessages()


  $.when(l10nXHR).done ->
    # Start the application
    App.start()

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
    height = Math.min 780, ($(window).height() - 20)
    el.innerHeight(height)


  App.requestComponentViewer = ->
    unless App.componentViewer?
      @resizeContainer()
      $(window).on 'resize', => @resizeContainer()
      App.componentViewer = new ComponentViewer
        elementToFit: el
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

      f = ->
        if drilldown.period?
          viewer.enablePeriod drilldown.period, activeHeaderItem
        else if activeHeaderItem?
          viewer.state.set activeHeaderTab: activeHeaderTab, activeHeaderItem: activeHeaderItem
          viewer.render()
        else viewer.showAllLines()

      viewer.open key
      viewer.on 'loaded', ->
        viewer.off 'loaded'
        if activeHeaderTab?
          viewer.headerView.enableBar(activeHeaderTab).done -> f()
        else f()



  # Message bundles
  l10nXHR = window.requestMessages()


  $.when(l10nXHR).done ->
    # Start the application
    App.start()

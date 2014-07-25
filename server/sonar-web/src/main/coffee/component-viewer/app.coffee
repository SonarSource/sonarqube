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
], (
  Marionette
  ComponentViewer
) ->

  $ = jQuery
  API_ISSUE = "#{baseUrl}/api/issues/show"
  App = new Marionette.Application()
  el = $('#body')


  App.addRegions
    viewerRegion: '#component-viewer'


  App.resizeContainer = ->
    width = $(window).width()
    height = $(window).height()
    el.innerWidth(width).innerHeight(height)


  App.requestComponentViewer = (s, currentIssue) ->
    if s?
      settings = issues: false, coverage: false, duplications: false, scm: false, workspace: false
      s.split(',').forEach (d) -> settings[d] = true
      settings.issues = false if currentIssue?
    else settings = null
    unless App.componentViewer?
      @resizeContainer()
      $(window).on 'resize', => @resizeContainer()
      App.componentViewer = new ComponentViewer
        settings: settings
        elementToFit: el
      App.viewerRegion.show App.componentViewer
    App.componentViewer


  App.addInitializer ->
    # Define parameters
    paramsHash = location.hash.substr(1)
    params = {}
    paramsHash.split('&').forEach (d) ->
      t = d.split '='
      params[t[0]] = decodeURIComponent t[1]

    viewer = App.requestComponentViewer params.settings, params.currentIssue
    if params.component?
      loadIssue = (key) ->
        $.get API_ISSUE, key: key, (r) =>
          viewer.showIssues false, r.issue

      if params.line?
        viewer.sourceView.highlightedLine = params.line
        viewer.on 'sized', ->
          viewer.off 'sized'
          viewer.scrollToLine params.line

      if params.blocks?
        blocks = params.blocks.split(';').map (b) ->
          t = b.split ','
          from: +t[0], to: +t[1]
        viewer.on 'resetShowBlocks', ->
          viewer.off 'resetShowBlocks'
          viewer.sourceView.showBlocks = blocks

      viewer.open params.component

      viewer.on 'loaded', ->
        viewer.off 'loaded'
        if params.tab? && params.item? && params.period?
          viewer.headerView.enableBar(params.tab).done ->
            viewer.enablePeriod +params.period, params.item
        else if params.tab? && params.item?
          viewer.state.set activeHeaderTab: params.tab, activeHeaderItem: params.item
          viewer.headerView.render()
        else if params.tab? && params.period?
          viewer.headerView.enableBar(params.tab).done ->
            viewer.enablePeriod params.period
        else if params.tab? && params.currentIssue?
          loadIssue(params.currentIssue).done ->
            viewer.state.set activeHeaderTab: params.tab
            viewer.headerView.render()
        else if params.tab?
          viewer.state.set activeHeaderTab: params.tab
          viewer.headerView.render()
          viewer.showAllLines()
        else if params.currentIssue?
          loadIssue params.currentIssue
        else viewer.showAllLines()



  # Message bundles
  l10nXHR = window.requestMessages()


  $.when(l10nXHR).done ->
    # Start the application
    App.start()

define [
  'backbone'
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/header'
  'component-viewer/source'
], (
  Backbone
  Marionette
  Templates
  HeaderView
  SourceView
) ->

  $ = jQuery
  API_SOURCES = "#{baseUrl}/api/sources"
  API_RESOURCES = "#{baseUrl}/api/resources"



  class ComponentViewer extends Marionette.Layout
    className: 'component-viewer'
    template: Templates['layout']


    regions:
      headerRegion: '.component-viewer-header'
      sourceRegion: '.component-viewer-source'


    initialize: ->
      @workspace = new Backbone.Collection()
      @component = new Backbone.Model()
      @headerView = new HeaderView
        model: @component
        main: @

      @source = new Backbone.Model()
      @sourceView = new SourceView model: @source, main: @

      @settings = new Backbone.Model issues: false, coverage: true, duplications: false


    onRender: ->
      @headerRegion.show @headerView
      @sourceRegion.show @sourceView


    requestComponent: (key, metrics) ->
      $.get API_RESOURCES, resource: key, metrics: metrics, (data) =>
        @component.set data[0]


    requestSource: (key) ->
      $.get API_SOURCES, resource: key, (data) =>
        @source.set source: data[0]


    extractCoverage: (data) ->
      toObj = (d) ->
        q = {}
        d.split(';').forEach (item) ->
          tokens = item.split '='
          q[tokens[0]] = tokens[1]
        q

      msr = data[0].msr
      coverage = toObj _.findWhere(msr, key: 'coverage_line_hits_data').data
      coverageConditions = toObj _.findWhere(msr, key: 'covered_conditions_by_line').data
      conditions = toObj _.findWhere(msr, key: 'conditions_by_line').data
      @source.set
        coverage: coverage
        coverageConditions: coverageConditions
        conditions: conditions


    open: (key) ->
      @workspace.reset [ key: key ]
      @_open key


    _open: (key) ->
      @key = key
      @sourceView.showSpinner()
      source = @requestSource key
      component = @requestComponent key
      $.when(source, component).done =>
        @workspace.where(key: key).forEach (model) =>
          model.set 'component': @component.toJSON()
        @render()
        if @settings.get('coverage') then @showCoverage() else @hideCoverage()


    showCoverage: ->
      unless @source.has 'coverage'
        metrics = 'coverage_line_hits_data,covered_conditions_by_line,conditions_by_line'
        @requestComponent(@key, metrics).done (data) =>
          @extractCoverage data
          @sourceView.render()
      else
        @sourceView.render()


    hideCoverage: ->
      @sourceView.hideCoverage()


    addTransition: (key, transition) ->
      @workspace.add key: key, transition: transition
      @_open key
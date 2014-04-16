define [
  'backbone'
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/source'
], (
  Backbone
  Marionette
  Templates
  SourceView
) ->

  $ = jQuery
  API_SOURCES = "#{baseUrl}/api/sources"
  API_RESOURCES = "#{baseUrl}/api/resources"



  class ComponentViewer extends Marionette.Layout
    className: 'component-viewer'
    template: Templates['layout']


    regions:
      sourceRegion: '.component-viewer-source'


    initialize: ->
      @source = new Backbone.Model()
      @sourceView = new SourceView model: @source


    onRender: ->
      @sourceRegion.show @sourceView


    requestSource: (key) ->
      $.get API_SOURCES, resource: key, (data) =>
        @source.set { source:  data[0] }, { silent: true }


    requestCoverage: (key) ->
      metrics = 'coverage_line_hits_data,covered_conditions_by_line,conditions_by_line'

      toObj = (data) ->
        q = {}
        data.split(';').forEach (item) ->
          tokens = item.split '='
          q[tokens[0]] = tokens[1]
        q


      $.get API_RESOURCES, resource: key, metrics: metrics, (data) =>
        msr = data[0].msr
        coverage = toObj _.findWhere(msr, key: 'coverage_line_hits_data').data
        coverageConditions = toObj _.findWhere(msr, key: 'covered_conditions_by_line').data
        conditions = toObj _.findWhere(msr, key: 'conditions_by_line').data
        @source.set {
          coverage: coverage,
          coverageConditions: coverageConditions
          conditions: conditions
        }, { silent: true }


    open: (key) ->
      $.when(@requestSource(key), @requestCoverage(key)).done =>
        @sourceView.render()
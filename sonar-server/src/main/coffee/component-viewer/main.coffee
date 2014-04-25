define [
  'backbone'
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/workspace'
  'component-viewer/source'
], (
  Backbone
  Marionette
  Templates
  WorkspaceView
  SourceView
) ->

  $ = jQuery
  API_SOURCES = "#{baseUrl}/api/sources"
  API_RESOURCES = "#{baseUrl}/api/resources"



  class ComponentViewer extends Marionette.Layout
    className: 'component-viewer'
    template: Templates['layout']


    regions:
      workspaceRegion: '.component-viewer-workspace'
      sourceRegion: '.component-viewer-source'


    initialize: ->
      @workspace = new Backbone.Collection()
      @component = new Backbone.Model()
      @workspaceView = new WorkspaceView
        collection: @workspace
        main: @

      @source = new Backbone.Model()
      @sourceView = new SourceView
        model: @source
        main: @

      @settings = new Backbone.Model
        issues: false
        coverage: false
        duplications: false
        scm: false
        workspace: false


    onRender: ->
      if @settings.get 'workspace'
        @workspaceRegion.show @workspaceView
        @$el.addClass 'component-viewer-workspace-enabled'
      else
        @$el.removeClass 'component-viewer-workspace-enabled'
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
      @settings.set 'coverage', true
      unless @source.has 'coverage'
        metrics = 'coverage_line_hits_data,covered_conditions_by_line,conditions_by_line'
        @requestComponent(@key, metrics).done (data) =>
          @extractCoverage data
          @sourceView.render()
      else
        @sourceView.render()


    hideCoverage: ->
      @settings.set 'coverage', false
      @sourceView.render()


    showWorkspace: ->
      @settings.set 'workspace', true
      @render()


    hideWorkspace: ->
      @settings.set 'workspace', false
      @render()


    showIssues: (issues) ->
      @settings.set 'issues', true
      if _.isArray(issues) && issues.length > 0
        @source.set 'issues', issues
      @sourceView.render()


    hideIssues: ->
      @settings.set 'issues', false
      @sourceView.render()


    addTransition: (key, transition, optionsForCurrent, options) ->
      if optionsForCurrent?
        last = @workspace.at(@workspace.length - 1)
        last.set 'options', optionsForCurrent if last
      @workspace.add key: key, transition: transition, options: options
      @_open key
define [
  'backbone'
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/workspace'
  'component-viewer/source'
  'component-viewer/header'

  'component-viewer/mockjax'
], (
  Backbone
  Marionette
  Templates
  WorkspaceView
  SourceView
  HeaderView
) ->

  $ = jQuery
  API_COMPONENT = "#{baseUrl}/api/sources/app"
  API_SOURCES = "#{baseUrl}/api/sources/show"
  API_SCM = "#{baseUrl}/api/sources/scm"



  class ComponentViewer extends Marionette.Layout
    className: 'component-viewer'
    template: Templates['layout']


    regions:
      workspaceRegion: '.component-viewer-workspace'
      headerRegion: '.component-viewer-header'
      sourceRegion: '.component-viewer-source'


    initialize: (options) ->
      @component = new Backbone.Model()
      @component.set options.component if options.component?

      @workspace = new Backbone.Collection()
      @workspaceView = new WorkspaceView
        collection: @workspace
        main: @

      @source = new Backbone.Model()
      @sourceView = new SourceView
        model: @source
        main: @

      @headerView = new HeaderView
        model: @source
        main: @

      @settings = new Backbone.Model
        issues: false
        coverage: false
        duplications: false
        scm: false
        workspace: false
      @settings.set options.settings


    onRender: ->
      if @settings.get 'workspace'
        @workspaceRegion.show @workspaceView
        @$el.addClass 'component-viewer-workspace-enabled'
      else
        @$el.removeClass 'component-viewer-workspace-enabled'
      @sourceRegion.show @sourceView
      @headerRegion.show @headerView


    requestComponent: (key) ->
      $.get API_COMPONENT, key: key, (data) =>
        @component.set data


    requestSource: (key) ->
      $.get API_SOURCES, key: key, (data) =>
        @source.set source: data.sources


    requestSCM: (key) ->
      $.get API_SCM, key: key, (data) =>
        @source.set scm: data.scm


    extractIssues: (data) ->
      issuesMeasures = {}
      data[0].msr.forEach (q) ->
        issuesMeasures[q.key] = q.frmt_val
      @component.set 'issuesMeasures', issuesMeasures


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
        if @settings.get('issues') then @showIssues() else @hideIssues()
        if @settings.get('coverage') then @showCoverage() else @hideCoverage()
        if @settings.get('duplications') then @showDuplications() else @hideDuplications()
        if @settings.get('scm') then @showSCM() else @hideSCM()


    showCoverage: ->
      @settings.set 'coverage', true
      @render()


    hideCoverage: ->
      @settings.set 'coverage', false
      @render()


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
      @render()


    hideIssues: ->
      @settings.set 'issues', false
      @render()


    showDuplications: ->
      @settings.set 'duplications', true
      @render()


    hideDuplications: ->
      @settings.set 'duplications', false
      @render()


    showSCM: ->
      @settings.set 'scm', true
      unless @source.has 'duplications'
        @requestSCM(@key).done => @sourceView.render()
      else
        @render()


    hideSCM: ->
      @settings.set 'scm', false
      @render()


    addTransition: (key, transition, optionsForCurrent, options) ->
      if optionsForCurrent?
        last = @workspace.at(@workspace.length - 1)
        last.set 'options', optionsForCurrent if last
      @workspace.add key: key, transition: transition, options: options
      @_open key
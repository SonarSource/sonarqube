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

  SOURCE_METRIC_LIST = 'lines,ncloc,functions,accessors,classes,statements,complexity,function_complexity,' +
                       'comment_lines_density,comment_lines,public_documented_api_density,public_undocumented_api,' +
                       'public_api'

  COVERAGE_METRIC_LIST = 'coverage,line_coverage,branch_coverage,' +
                         'coverage_line_hits_data,covered_conditions_by_line,conditions_by_line'

  ISSUES_METRIC_LIST = 'blocker_violations,critical_violations,major_violations,minor_violations,info_violations'

  DUPLICATIONS_METRIC_LIST = 'duplicated_lines_density,duplicated_blocks,duplicated_files,duplicated_lines'



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
        duplications: true
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


    requestComponentCoverage: (key) ->
      $.get API_RESOURCES, resource: key, metrics: COVERAGE_METRIC_LIST


    requestComponentIssues: (key) ->
      $.get API_RESOURCES, resource: key, metrics: ISSUES_METRIC_LIST


    requestComponentDuplications: (key) ->
      $.get API_RESOURCES, resource: key, metrics: DUPLICATIONS_METRIC_LIST


    requestSource: (key) ->
      $.get API_SOURCES, resource: key, (data) =>
        @source.set source: data[0]


    extractCoverage: (data) ->
      toObj = (d) ->
        q = {}
        d?.split(';').forEach (item) ->
          tokens = item.split '='
          q[tokens[0]] = tokens[1]
        q

      msr = data[0].msr
      coverage = toObj _.findWhere(msr, key: 'coverage_line_hits_data')?.data
      coverageConditions = toObj _.findWhere(msr, key: 'covered_conditions_by_line')?.data
      conditions = toObj _.findWhere(msr, key: 'conditions_by_line')?.data
      @source.set
        coverage: coverage
        coverageConditions: coverageConditions
        conditions: conditions
      coverageMeasures = _.reject data[0].msr, (m) ->
        m.key == 'coverage_line_hits_data' || m.key == 'covered_conditions_by_line' || m.key == 'conditions_by_line'
      @component.set 'coverageMeasures', coverageMeasures


    extractDuplications: (data) ->
      @source.set
        duplications: [
          { from: 18, count: 33 }
          { from: 24, count: 23 }
          { from: 62, count: 33 }
        ]
      duplicationsMeasures = _.sortBy data[0].msr, (item) -> -(item.key == 'duplicated_lines_density')
      console.log data
      @component.set 'duplicationsMeasures', duplicationsMeasures


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
      component = @requestComponent key, SOURCE_METRIC_LIST
      $.when(source, component).done =>
        @workspace.where(key: key).forEach (model) =>
          model.set 'component': @component.toJSON()
        @render()
        if @settings.get('coverage') then @showCoverage() else @hideCoverage()
        if @settings.get('duplications') then @showDuplications() else @hideDuplications()


    showCoverage: ->
      @settings.set 'coverage', true
      unless @source.has 'coverage'
        @requestComponentCoverage(@key).done (data) =>
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

      unless @source.has 'issues'
        @requestComponentIssues(@key).done (data) =>
          @extractIssues data
          @sourceView.render()

      if _.isArray(issues) && issues.length > 0
        @source.set 'issues', issues
      @sourceView.render()


    hideIssues: ->
      @settings.set 'issues', false
      @sourceView.render()


    showDuplications: ->
      @settings.set 'duplications', true
      unless @source.has 'duplications'
        @requestComponentDuplications(@key).done (data) =>
          @extractDuplications data
          @sourceView.render()
      else
        @sourceView.render()


    hideDuplications: ->
      @settings.set 'duplications', false
      @sourceView.render()


    addTransition: (key, transition, optionsForCurrent, options) ->
      if optionsForCurrent?
        last = @workspace.at(@workspace.length - 1)
        last.set 'options', optionsForCurrent if last
      @workspace.add key: key, transition: transition, options: options
      @_open key
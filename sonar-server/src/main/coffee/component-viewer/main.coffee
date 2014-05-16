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

  API_COMPONENT = "#{baseUrl}/api/components/app"
  API_SOURCES = "#{baseUrl}/api/sources/show"
  API_COVERAGE = "#{baseUrl}/api/coverage/show"
  API_SCM = "#{baseUrl}/api/sources/scm"
  API_MEASURES = "#{baseUrl}/api/resources"

  LINES_AROUND_ISSUE = 4
  LINES_AROUND_COVERED_LINE = 1

  SOURCE_METRIC_LIST = 'lines,ncloc,functions,accessors,classes,statements,complexity,function_complexity,' +
    'comment_lines_density,comment_lines,public_documented_api_density,public_undocumented_api,' +
    'public_api'

  COVERAGE_METRIC_LIST = 'coverage,line_coverage,lines_to_cover,covered_lines,uncovered_lines,' +
    'branch_coverage,conditions_to_cover,uncovered_conditions,' +
    'it_coverage,it_line_coverage,it_lines_to_cover,it_covered_lines,it_uncovered_lines,' +
    'it_branch_coverage,it_conditions_to_cover,it_uncovered_conditions'

  ISSUES_METRIC_LIST = 'blocker_violations,critical_violations,major_violations,minor_violations,info_violations,' +
    'false_positive_issues'

  DUPLICATIONS_METRIC_LIST = 'duplicated_lines_density,duplicated_blocks,duplicated_files,duplicated_lines'



  class ComponentViewer extends Marionette.Layout
    className: 'component-viewer'
    template: Templates['layout']


    regions:
      workspaceRegion: '.component-viewer-workspace'
      headerRegion: '.component-viewer-header'
      sourceRegion: '.component-viewer-source'


    initialize: (options) ->
      @settings = new Backbone.Model
        issues: false
        coverage: false
        duplications: false
        scm: false
        workspace: false
      @settings.set options.settings

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


    requestMeasures: (key) ->
      metrics = [SOURCE_METRIC_LIST, COVERAGE_METRIC_LIST, ISSUES_METRIC_LIST, DUPLICATIONS_METRIC_LIST].join ','
      $.get API_MEASURES, resource: key, metrics: metrics, (data) =>
        measuresList = data[0].msr || []
        measures = {}
        measuresList.forEach (m) -> measures[m.key] = m.frmt_val

        if measures['lines_to_cover']? && measures['uncovered_lines']?
          measures['covered_lines'] = measures['lines_to_cover'] - measures['uncovered_lines']

        if measures['conditions_to_cover']? && measures['uncovered_conditions']?
          measures['covered_conditions'] = measures['conditions_to_cover'] - measures['uncovered_conditions']

        if measures['it_lines_to_cover']? && measures['it_uncovered_lines']?
          measures['it_covered_lines'] = measures['it_lines_to_cover'] - measures['it_uncovered_lines']

        if measures['it_conditions_to_cover']? && measures['it_uncovered_conditions']?
          measures['it_covered_conditions'] = measures['it_conditions_to_cover'] - measures['it_uncovered_conditions']

        @component.set 'msr', measures


    requestSource: (key) ->
      $.get API_SOURCES, key: key, (data) =>
        @source.set source: data.sources


    requestSCM: (key) ->
      $.get API_SCM, key: key, (data) =>
        @source.set scm: data.scm


    requestCoverage: (key, type = 'UT') ->
      $.get API_COVERAGE, key: key, type: type, (data) =>
        @source.set coverage: data.coverage


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
      unless @source.has 'coverage'
        @requestCoverage(@key).done => @sourceView.render()
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
        @filterLinesByIssues()
      else
        @sourceView.render()


    hideIssues: ->
      @settings.set 'issues', false
      @sourceView.render()


    showDuplications: ->
      @settings.set 'duplications', true
      @sourceView.render()


    hideDuplications: ->
      @settings.set 'duplications', false
      @sourceView.render()


    showSCM: ->
      @settings.set 'scm', true
      unless @source.has 'scm'
        @requestSCM(@key).done => @sourceView.render()
      else
        @sourceView.render()


    hideSCM: ->
      @settings.set 'scm', false
      @sourceView.render()


    filterLinesByIssues: ->
      issues = @source.get 'issues'
      @sourceView.resetShowBlocks()
      issues.forEach (issue) =>
        line = issue.line || 0
        @sourceView.addShowBlock line - LINES_AROUND_ISSUE, line + LINES_AROUND_ISSUE
      @sourceView.render()


    filterByCoverage: (predicate) ->
      @requestCoverage(@key).done => @_filterByCoverage(predicate)


    filterByCoverageIT: (predicate) ->
      @requestCoverage(@key, 'IT').done => @_filterByCoverage(predicate)


    _filterByCoverage: (predicate) ->
      coverage = @source.get 'coverage'
      @settings.set 'coverage', true
      @sourceView.resetShowBlocks()
      coverage.forEach (c) =>
        if predicate c
          line = c[0]
          @sourceView.addShowBlock line - LINES_AROUND_COVERED_LINE, line + LINES_AROUND_COVERED_LINE
      @sourceView.render()


    # Unit Tests
    filterByLinesToCover: -> @filterByCoverage (c) -> c[1]?
    filterByCoveredLines: -> @filterByCoverage (c) -> c[1]? && c[1]
    filterByUncoveredLines: -> @filterByCoverage (c) -> c[1]? && !c[1]
    filterByBranchesToCover: -> @filterByCoverage (c) -> c[3]?
    filterByCoveredBranches: -> @filterByCoverage (c) -> c[3]? && c[4]? && (c[4] > 0)
    filterByUncoveredBranches: -> @filterByCoverage (c) -> c[3]? && c[4]? && (c[3] > c[4])

    # Integration Tests
    filterByLinesToCoverIT: -> @filterByCoverageIT (c) -> c[1]?
    filterByCoveredLinesIT: -> @filterByCoverageIT (c) -> c[1]? && c[1]
    filterByUncoveredLinesIT: -> @filterByCoverageIT (c) -> c[1]? && !c[1]
    filterByBranchesToCoverIT: -> @filterByCoverageIT (c) -> c[3]?
    filterByCoveredBranchesIT: -> @filterByCoverageIT (c) -> c[3]? && c[4]? && (c[4] > 0)
    filterByUncoveredBranchesIT: -> @filterByCoverageIT (c) -> c[3]? && c[4]? && (c[3] > c[4])





    addTransition: (key, transition, optionsForCurrent, options) ->
      if optionsForCurrent?
        last = @workspace.at(@workspace.length - 1)
        last.set 'options', optionsForCurrent if last
      @workspace.add key: key, transition: transition, options: options
      @_open key
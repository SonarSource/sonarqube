define [
  'backbone'
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/workspace'
  'component-viewer/source'
  'component-viewer/header'
  'component-viewer/utils'

  'component-viewer/mockjax'
], (
  Backbone
  Marionette
  Templates
  WorkspaceView
  SourceView
  HeaderView
  utils
) ->

  $ = jQuery

  API_COMPONENT = "#{baseUrl}/api/components/app"
  API_SOURCES = "#{baseUrl}/api/sources/show"
  API_ISSUES = "#{baseUrl}/api/issues/search"
  API_COVERAGE = "#{baseUrl}/api/coverage/show"
  API_SCM = "#{baseUrl}/api/sources/scm"
  API_MEASURES = "#{baseUrl}/api/resources"
  API_DUPLICATIONS = "#{baseUrl}/api/duplications/show"

  LINES_AROUND_ISSUE = 4
  LINES_AROUND_COVERED_LINE = 1
  LINES_AROUND_DUPLICATION = 1

  SOURCE_METRIC_LIST = 'accessors,classes,functions,statements,' +
    'ncloc,lines,' +
    'complexity,function_complexity,' +
    'comment_lines,comment_lines_density,public_api,public_undocumented_api,public_documented_api_density'

  COVERAGE_METRIC_LIST = 'coverage,line_coverage,lines_to_cover,covered_lines,uncovered_lines,' +
    'branch_coverage,conditions_to_cover,uncovered_conditions,' +
    'it_coverage,it_line_coverage,it_lines_to_cover,it_covered_lines,it_uncovered_lines,' +
    'it_branch_coverage,it_conditions_to_cover,it_uncovered_conditions'

  ISSUES_METRIC_LIST = 'blocker_violations,critical_violations,major_violations,minor_violations,info_violations,' +
    'false_positive_issues'

  DUPLICATIONS_METRIC_LIST = 'duplicated_lines_density,duplicated_blocks,duplicated_files,duplicated_lines'

  TESTS_METRIC_LIST = 'tests'



  class ComponentViewer extends Marionette.Layout
    className: 'component-viewer'
    template: Templates['layout']


    regions:
      workspaceRegion: '.component-viewer-workspace'
      headerRegion: '.component-viewer-header'
      sourceRegion: '.component-viewer-source'


    initialize: (options) ->
      @settings = new Backbone.Model @getDefaultSettings()
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

      @requestIssuesOnce = false


    getDefaultSettings: ->
      componentViewerSettings = localStorage.getItem 'componentViewerSettings'
      if componentViewerSettings? then JSON.parse componentViewerSettings else
        issues: false
        coverage: false
        duplications: false
        scm: false
        workspace: false


    storeSettings: ->
      localStorage.setItem 'componentViewerSettings', JSON.stringify @settings.toJSON()


    onRender: ->
      @workspaceRegion.show @workspaceView
      @$el.toggleClass 'component-viewer-workspace-enabled', @settings.get 'workspace'
      @sourceRegion.show @sourceView
      @headerRegion.show @headerView


    requestComponent: (key) ->
      $.get API_COMPONENT, key: key, (data) =>
        @component.set data
        @component.set 'dir', utils.splitLongName(data.path).dir


    requestMeasures: (key) ->
      unless @component.get('q') == 'UTS'
        metrics = [SOURCE_METRIC_LIST, COVERAGE_METRIC_LIST, ISSUES_METRIC_LIST, DUPLICATIONS_METRIC_LIST].join ','
      else
        metrics = [ISSUES_METRIC_LIST, TESTS_METRIC_LIST]
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
        @source.clear()
        @source.set source: data.sources


    requestSCM: (key) ->
      $.get API_SCM, key: key, (data) =>
        @source.set scm: data.scm


    requestIssues: (key) ->
      options =
        components: key
        ps: 10000
        extra_fields: 'actions,transitions,assigneeName,actionPlanName'
      $.get API_ISSUES, options, (data) =>
        @requestIssuesOnce = true
        @source.set issues: data.issues


    requestCoverage: (key, type = 'UT') ->
      $.get API_COVERAGE, key: key, type: type, (data) =>
        @source.set coverage: data.coverage


    requestDuplications: (key) ->
      $.get API_DUPLICATIONS, key: key, (data) =>
        @source.set duplications: data.duplications
        @source.set duplicationFiles: data.files


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


    showCoverage: (store = false) ->
      @settings.set 'coverage', true
      @storeSettings() if store
      unless @source.has 'coverage'
        @requestCoverage(@key).done => @sourceView.render()
      else
        @sourceView.render()


    hideCoverage: (store = false) ->
      @settings.set 'coverage', false
      @storeSettings() if store
      @sourceView.render()


    toggleWorkspace: (store = false) ->
      if @settings.get 'workspace' then @hideWorkspace() else @showWorkspace()
      @storeSettings() if store


    showWorkspace: (store = false) ->
      @settings.set 'workspace', true
      @storeSettings() if store
      @render()


    hideWorkspace: (store = false) ->
      @settings.set 'workspace', false
      @storeSettings() if store
      @render()


    showIssues: (store = false, issue) ->
      @settings.set 'issues', true
      @storeSettings() if store
      if issue?
        @currentIssue = issue.key
        @source.set 'issues', [issue]
        @filterByCurrentIssue()
      else
        @sourceView.render()


    hideIssues: (store = false) ->
      @settings.set 'issues', false
      @storeSettings() if store
      @sourceView.render()


    showDuplications: (store = false) ->
      @settings.set 'duplications', true
      @storeSettings() if store
      unless @source.has 'duplications'
        @requestDuplications(@key).done => @sourceView.render()
      else
        @sourceView.render()


    hideDuplications: (store = false) ->
      @settings.set 'duplications', false
      @storeSettings() if store
      @sourceView.render()


    showSCM: (store = false) ->
      @settings.set 'scm', true
      @storeSettings() if store
      unless @source.has 'scm'
        @requestSCM(@key).done => @sourceView.render()
      else
        @sourceView.render()


    hideSCM: (store = false) ->
      @settings.set 'scm', false
      @storeSettings() if store
      @sourceView.render()


    showAllLines: ->
      @sourceView.resetShowBlocks()
      @sourceView.showBlocks.push from: 0, to: _.size @source.get 'source'
      @sourceView.render()


    filterLinesByIssues: ->
      issues = @source.get 'issues'
      @sourceView.resetShowBlocks()
      issues.forEach (issue) =>
        line = issue.line || 0
        @sourceView.addShowBlock line - LINES_AROUND_ISSUE, line + LINES_AROUND_ISSUE
      @sourceView.render()


    filterByIssues: (predicate, requestIssues = true) ->
      if requestIssues && !@requestIssuesOnce
        @requestIssues(@key).done => @_filterByIssues(predicate)
      else
        @_filterByIssues(predicate)


    _filterByIssues: (predicate) ->
      issues = @source.get 'issues'
      @settings.set 'issues', true
      @sourceView.resetShowBlocks()
      activeIssues = []
      issues.forEach (issue) =>
        if predicate issue
          line = issue.line || 0
          @sourceView.addShowBlock line - LINES_AROUND_ISSUE, line + LINES_AROUND_ISSUE
          activeIssues.push issue
      @source.set 'activeIssues', activeIssues
      @sourceView.render()


    # Current Issue
    filterByCurrentIssue: -> @filterByIssues ((issue) => issue.key == @currentIssue), false

    # All Issues
    filterByAllIssues: -> @filterByIssues -> true

    # Resolved Issues
    filterByResolvedIssues: -> @filterByIssues (issue) -> !!issue.resolution

    # Unresolved Issues
    filterByUnresolvedIssues: -> @filterByIssues (issue) -> !issue.resolution

    # False Positive
    filterByFalsePositiveIssues: -> @filterByIssues (issue) -> issue.resolution == 'FALSE-POSITIVE'

    # Rule
    filterByRule: (rule) -> @filterByIssues (issue) -> issue.rule == rule

    # Severity
    filterByBlockerIssues: -> @filterByIssues (issue) -> issue.severity == 'BLOCKER' && !issue.resolution
    filterByCriticalIssues: -> @filterByIssues (issue) -> issue.severity == 'CRITICAL' && !issue.resolution
    filterByMajorIssues: -> @filterByIssues (issue) -> issue.severity == 'MAJOR' && !issue.resolution
    filterByMinorIssues: -> @filterByIssues (issue) -> issue.severity == 'MINOR' && !issue.resolution
    filterByInfoIssues: -> @filterByIssues (issue) -> issue.severity == 'INFO' && !issue.resolution


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


    # Duplications
    filterByDuplications: ->
      unless @source.has 'duplications'
        @requestDuplications(@key).done => @_filterByDuplications()
      else
        @_filterByDuplications()


    _filterByDuplications: ->
      duplications = @source.get 'duplications'
      @settings.set 'duplications', true
      @sourceView.resetShowBlocks()
      duplications.forEach (d) =>
        lineFrom = d.blocks[0].from
        lineTo = lineFrom + d.blocks[0].size
        @sourceView.addShowBlock lineFrom - LINES_AROUND_DUPLICATION, lineTo + LINES_AROUND_DUPLICATION
      @sourceView.render()


    addTransition: (key, transition, optionsForCurrent, options) ->
      if optionsForCurrent?
        last = @workspace.at(@workspace.length - 1)
        last.set 'options', optionsForCurrent if last
      @workspace.add key: key, transition: transition, options: options
      @_open key
      @showAllLines()
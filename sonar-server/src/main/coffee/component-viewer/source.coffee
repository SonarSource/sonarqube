define [
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/coverage-popup'
  'component-viewer/duplication-popup'
  'component-viewer/time-changes-popup'
  'issues/issue-view'
  'issues/models/issue'
  'common/handlebars-extensions'
], (
  Marionette
  Templates
  CoveragePopupView
  DuplicationPopupView
  TimeChangesPopupView
  IssueView
  Issue
) ->

  $ = jQuery

  API_COVERAGE_TESTS = "#{baseUrl}/api/tests/test_cases"


  class SourceView extends Marionette.ItemView
    template: Templates['source']
    expandTemplate: Templates['code-expand']

    LINES_AROUND_ISSUE = 4
    LINES_AROUND_COVERED_LINE = 1
    EXPAND_LINES = 20


    events:
      'click .sym': 'highlightUsages'

      'click .coverage-tests': 'showCoveragePopup'

      'click .duplication-exists': 'showDuplicationPopup'
      'mouseenter .duplication-exists': 'duplicationMouseEnter'
      'mouseleave .duplication-exists': 'duplicationMouseLeave'

      'click .js-expand': 'expandBlock'
      'click .js-expand-all': 'expandAll'

      'click .js-time-changes': 'toggleTimeChangePopup'


    initialize: ->
      super
      @showBlocks = []


    resetShowBlocks: ->
      @showBlocks = []


    addShowBlock: (from, to) ->
      @showBlocks.push from: from, to: to


    onRender: ->
      @delegateEvents()
      @showSettings = false
      @renderExpandButtons()
      @renderIssues() if @options.main.settings.get('issues') && @model.has('activeIssues')


    renderExpandButtons: ->
      rows = @$('.row[data-line-number]')
      rows.get().forEach (row) =>
        line = $(row).data 'line-number'
        linePrev = $(row).prev('[data-line-number]').data 'line-number'
        if line? && linePrev? && (linePrev + 1) < line
          expand = @expandTemplate from: linePrev, to: line, settings: @options.main.settings.toJSON()
          $(expand).insertBefore $(row)

      lines = _.size @model.get 'source'
      lastShown = rows.last().data('line-number')
      if lastShown < lines
        expand = @expandTemplate from: lastShown, to: lines, settings: @options.main.settings.toJSON()
        $(expand).insertAfter rows.last()

      @delegateEvents()


    renderIssues: ->
      issues = @model.get 'activeIssues'
      issues.forEach (issue) =>
        line = issue.line || 0
        row = @$("[data-line-number=#{line}]")
        row.removeClass 'row-hidden'
        container = row.children('.line')
        container.addClass 'issue' if line > 0
        issueView = new IssueView model: new Issue issue
        issueView.render().$el.appendTo container
        issueView.on 'reset', =>
          @options.main.requestComponent(@options.main.key, false).done =>
            @options.main.headerView.render()
            @options.main.headerView.$('.component-viewer-header-measures-expand[data-scope=issues]').click()


    showSpinner: ->
      @$el.html '<div style="padding: 10px;"><i class="spinner"></i></div>'


    highlightUsages: (e) ->
      key = e.currentTarget.className.split(/\s+/)[0]
      @$('.sym.highlighted').removeClass 'highlighted'
      @$(".sym.#{key}").addClass 'highlighted'


    toggleSettings: ->
      @$('.settings-toggle button').toggleClass 'open'
      @$('.component-viewer-source-settings').toggleClass 'open'


    toggleMeasures: (e) ->
      row = $(e.currentTarget).closest '.component-viewer-header'
      row.toggleClass 'component-viewer-header-full'


    showCoveragePopup: (e) ->
      e.stopPropagation()
      $('body').click()
      line = $(e.currentTarget).closest('.row').data 'line-number'
      $.get API_COVERAGE_TESTS, key: @options.main.component.get('key'), line: line, (data) =>
        popup = new CoveragePopupView
          model: new Backbone.Model data
          triggerEl: $(e.currentTarget)
          main: @options.main
        popup.render()


    showDuplicationPopup: (e) ->
      e.stopPropagation()
      $('body').click()
      index = $(e.currentTarget).data 'index'
      popup = new DuplicationPopupView
        triggerEl: $(e.currentTarget)
        main: @options.main
        collection: new Backbone.Collection @model.get('duplications')[index - 1].blocks
      popup.render()


    duplicationMouseEnter: (e) ->
      @toggleDuplicationHover e, true


    duplicationMouseLeave: (e) ->
      @toggleDuplicationHover e, false


    toggleDuplicationHover: (e, add) ->
      bar = $(e.currentTarget)
      index = bar.parent().children('.duplication').index bar
      @$('.duplications').each ->
        $(".duplication", @).eq(index).filter('.duplication-exists').toggleClass 'duplication-hover', add


    expandBlock: (e) ->
      linesFrom = $(e.currentTarget).data 'from'
      linesTo = $(e.currentTarget).data 'to'
      if linesTo == _.size @model.get 'source'
        if linesTo - linesFrom > EXPAND_LINES
          linesTo = linesFrom + EXPAND_LINES
      if linesFrom == 0 && linesTo > EXPAND_LINES
        linesFrom = linesTo - EXPAND_LINES
      @showBlocks.push from: linesFrom, to: linesTo
      @render()


    expandAll: ->
      @options.main.showAllLines()


    toggleTimeChangePopup: (e) ->
      e.stopPropagation()
      $('body').click()
      popup = new TimeChangesPopupView
        triggerEl: $(e.currentTarget)
        main: @options.main
        bottom: true
      popup.render()


    augmentWithCoverage: (source) ->
      coverage = @model.get 'coverage'
      if coverage
        coverage.forEach (s) ->
          line = source[s[0] - 1]
          line.coverage =
            covered: s[1]
            testCases: s[2]
            branches: s[3]
            coveredBranches: s[4]
          if line.coverage.branches? && line.coverage.coveredBranches?
            line.coverage.branchCoverageStatus = 'green' if line.coverage.branches == line.coverage.coveredBranches
            line.coverage.branchCoverageStatus = 'orange' if line.coverage.branches > line.coverage.coveredBranches
            line.coverage.branchCoverageStatus = 'red' if line.coverage.coveredBranches == 0
      source


    augmentWithDuplications: (source) ->
      duplications = @model.get 'duplications'
      return source unless duplications?
      source.forEach (line) ->
        lineDuplications = []
        duplications.forEach (d, i) ->
          duplicated = false
          d.blocks.forEach (b) ->
            if b._ref == '1'
              lineFrom = b.from
              lineTo = b.from + b.size
              duplicated = true if line.lineNumber >= lineFrom && line.lineNumber <= lineTo
          lineDuplications.push if duplicated then i + 1 else false
        line.duplications = lineDuplications
      source


    getSCMForLine: (lineNumber) ->
      scm = @model.get('scm') || []
      closest = -1
      closestIndex = -1
      scm.forEach (s, i) ->
        line = s[0]
        if line <= lineNumber && line > closest
          closest = line
          closestIndex = i
      if closestIndex != -1 then scm[closestIndex] else null


    augmentWithSCM: (source) ->
      scm = @model.get('scm') || []
      scm.forEach (s) ->
        line = _.findWhere source, lineNumber: s[0]
        line.scm = author: s[1], date: s[2]
      @showBlocks.forEach (block) =>
        scmForLine = @getSCMForLine block.from
        if scmForLine?
          line = _.findWhere source, lineNumber: block.from
          line.scm = author: scmForLine[1], date: scmForLine[2]
      source


    augmentWithShow: (source) ->
      source.forEach (sourceLine) =>
        show = false
        line = sourceLine.lineNumber
        @showBlocks.forEach (block) ->
          show = true if block.from <= line && block.to >= line
        _.extend sourceLine, show: show
      source


    prepareSource: ->
      source = @model.get 'source'
      source = _.map source, (item) =>
        lineNumber: item[0], code: item[1]

      if @options.main.settings.get 'coverage'
        source = @augmentWithCoverage source
      if @options.main.settings.get 'duplications'
        source = @augmentWithDuplications source
      if @options.main.settings.get 'scm'
        source = @augmentWithSCM source

      @augmentWithShow source


    getStatColumnsCount: ->
      count = 1 # line number
      count += 2 if @options.main.settings.get 'coverage'
      count += 1 if @options.main.settings.get 'duplications'
      count += 1 if @options.main.settings.get 'issues'
      count


    serializeData: ->
      source: @prepareSource()
      settings: @options.main.settings.toJSON()
      showSettings: @showSettings
      component: @options.main.component.toJSON()
      columns: @getStatColumnsCount() + 1
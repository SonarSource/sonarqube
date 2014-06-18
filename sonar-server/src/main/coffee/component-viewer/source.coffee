define [
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/coverage-popup'
  'component-viewer/duplication-popup'
  'component-viewer/time-changes-popup'
  'component-viewer/line-actions-popup'
  'issues/issue-view'
  'issues/models/issue'
  'common/handlebars-extensions'
], (
  Marionette
  Templates
  CoveragePopupView
  DuplicationPopupView
  TimeChangesPopupView
  LineActionsPopupView
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
    HIGHLIGHTED_ROW_CLASS = 'row-highlighted'


    events:
      'click .sym': 'highlightUsages'

      'click .lid': 'highlightLine'

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


    addShowBlock: (from, to, forceIncludeZero = false) ->
      if from <= 0 && !forceIncludeZero
        from = 1
      @showBlocks.push from: from, to: to


    onRender: ->
      @delegateEvents()
      @showSettings = false
      @renderExpandButtons()
      @renderIssues() if @options.main.settings.get('issues') && @model.has('activeIssues')
      @highlightCurrentLine()


    renderExpandButtons: ->
      rows = @$('.row[data-line-number]')
      rows.get().forEach (row) =>
        line = $(row).data 'line-number'
        linePrev = $(row).prev('[data-line-number]').data 'line-number'
        if line? && linePrev? && (linePrev + 1) < line
          expand = @expandTemplate from: linePrev, to: line, settings: @options.main.settings.toJSON()
          $(expand).insertBefore $(row)

      firstShown = rows.first().data('line-number')
      if firstShown > 1
        expand = @expandTemplate from: firstShown - EXPAND_LINES, to: firstShown, settings: @options.main.settings.toJSON()
        $(expand).insertBefore rows.first()

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
        if row.length > 0
          row.removeClass 'row-hidden'
          container = row.children('.line')
          container.addClass 'issue' if line > 0
          issueView = new IssueView model: new Issue issue
          issueView.render().$el.appendTo container
          issueView.on 'reset', =>
            @options.main.requestComponent(@options.main.key, false, false).done =>
              @options.main.headerView.render()


    showSpinner: ->
      @$el.html '<div style="padding: 10px;"><i class="spinner"></i></div>'


    showLineActionsPopup: (e) ->
      e.stopPropagation()
      $('body').click()
      popup = new LineActionsPopupView
        triggerEl: $(e.currentTarget)
        main: @options.main
        row: $(e.currentTarget).closest '.row'
      popup.render()


    highlightLine: (e) ->
      @$(".#{HIGHLIGHTED_ROW_CLASS}").removeClass HIGHLIGHTED_ROW_CLASS
      row = $(e.currentTarget).closest('.row')
      row.addClass HIGHLIGHTED_ROW_CLASS
      @highlightedLine = row.data 'line-number'
      @showLineActionsPopup(e)


    highlightCurrentLine: ->
      if @highlightedLine?
        @$("[data-line-number=#{@highlightedLine}]").addClass HIGHLIGHTED_ROW_CLASS


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
      line = $(e.currentTarget).closest('[data-line-number]').data 'line-number'
      blocks = @model.get('duplications')[index - 1].blocks
      blocks = _.filter blocks, (b) ->
        (b._ref != '1') || (b._ref == '1' && b.from > line) || (b._ref == '1' && b.from + b.size <= line)
      popup = new DuplicationPopupView
        triggerEl: $(e.currentTarget)
        main: @options.main
        collection: new Backbone.Collection blocks
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
      source = @model.get 'formattedSource'
      if source?
        @augmentWithShow source


    getStatColumnsCount: ->
      count = 1 # line number
      count += 2 if @options.main.settings.get 'coverage'
      count += 1 if @options.main.settings.get 'duplications'
      count += 1 if @options.main.settings.get 'issues'
      count


    showZeroLine: ->
      r = false
      @showBlocks.forEach (block) ->
        r = true if block.from <= 0
      r


    serializeData: ->
      source: @prepareSource()
      settings: @options.main.settings.toJSON()
      showSettings: @showSettings
      component: @options.main.component.toJSON()
      columns: @getStatColumnsCount() + 1
      showZeroLine: @showZeroLine()
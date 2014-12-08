define [
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/coverage-popup'
  'component-viewer/duplication-popup'
  'component-viewer/time-changes-popup'
  'component-viewer/line-actions-popup'
  'issue/issue-view'
  'issue/models/issue'
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
  LINES_LIMIT = 3000
  ISSUES_LIMIT = 100


  class SourceView extends Marionette.ItemView
    template: Templates['cw-source']
    expandTemplate: Templates['cw-code-expand']

    LINES_AROUND_ISSUE = 4
    LINES_AROUND_COVERED_LINE = 1
    EXPAND_LINES = 20
    HIGHLIGHTED_ROW_CLASS = 'source-line-highlighted'


    events:
      'click .sym': 'highlightUsages'

      'click .js-line-actions': 'highlightLine'

      'click .source-line-covered': 'showCoveragePopup'
      'click .source-line-partially-covered': 'showCoveragePopup'
      'click .source-line-uncovered': 'showCoveragePopup'

      'click .source-line-duplications-extra': 'showDuplicationPopup'

      'click .js-expand': 'expandBlock'
      'click .js-expand-all': 'expandAll'

      'click .js-time-changes': 'toggleTimeChangePopup'


    initialize: ->
      super
      @showBlocks = []


    resetShowBlocks: ->
      @showBlocks = []
      @options.main.trigger 'resetShowBlocks'


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
      rows = @$('.source-line[data-line-number]')
      rows.get().forEach (row) =>
        line = $(row).data 'line-number'
        linePrev = $(row).prev('[data-line-number]').data 'line-number'
        if line? && linePrev? && (linePrev + 1) < line
          expand = @expandTemplate
            from: linePrev
            to: line
            settings: @options.main.settings.toJSON()
            baseDuplications: @getBaseDuplications()
          $(expand).insertBefore $(row)

      firstShown = rows.first().data('line-number')
      if firstShown > 1
        expand = @expandTemplate
          from: firstShown - EXPAND_LINES
          to: firstShown
          settings: @options.main.settings.toJSON()
          baseDuplications: @getBaseDuplications()
        $(expand).insertBefore rows.first()

      lines = _.size @model.get 'source'
      lines = Math.min lines, LINES_LIMIT
      lastShown = rows.last().data('line-number')
      if lastShown < lines
        expand = @expandTemplate
          from: lastShown
          to: lines
          settings: @options.main.settings.toJSON()
          baseDuplications: @getBaseDuplications()
        $(expand).insertAfter rows.last()

      @delegateEvents()


    renderIssues: ->
      issues = @model.get 'activeIssues'
      issues = _.sortBy issues, 'line'
      rendered = 0
      issues.forEach (issue) =>
        line = issue.line || 0
        line = 0 if issue.status == 'CLOSED'
        row = @$("##{@cid}-#{line}")
        unless row.length > 0
          line = 0
          row = @$("##{@cid}-#{line}")
        if row.length > 0
          rendered += 1
          row.removeClass 'hidden'
          container = row.children('.source-line-code')
          container.addClass 'has-issues' if line > 0
          if rendered < ISSUES_LIMIT
            issueModel = new Issue issue
            issueView = new IssueView model: issueModel
            issues = container.find '.issue-list'
            if issues.length == 0
              issues = $('<div class="issue-list"></div>').appendTo container
            issueView.render().$el.appendTo issues
            issueView.$el.prop('id', "issue-#{issue.key}").data('issue-key', issue.key)
            issueView.on 'reset', =>
              @updateIssue issueModel
              @options.main.requestComponent(@options.main.key, false, false).done =>
                @options.main.headerView.silentUpdate = true
                @options.main.headerView.render()
          else
            row.prop 'title', tp('component_viewer.issues_limit_reached_tooltip', issue.message)


    updateIssue: (issueModel) ->
      issues = @model.get 'issues'
      issues = _.reject issues, (issue) -> issue.key == issueModel.get('key')
      issues.push issueModel.toJSON()
      @model.set 'issues', issues

      issues = @model.get 'activeIssues'
      issues = _.reject issues, (issue) -> issue.key == issueModel.get('key')
      issues.push issueModel.toJSON()
      @model.set 'activeIssues', issues


    showSpinner: ->
      @$el.html '<div style="padding: 10px;"><i class="spinner"></i></div>'


    showLineActionsPopup: (e) ->
      e.stopPropagation()
      $('body').click()
      popup = new LineActionsPopupView
        triggerEl: $(e.currentTarget)
        main: @options.main
        row: $(e.currentTarget).closest '.source-line'
      popup.render()


    highlightLine: (e) ->
      row = $(e.currentTarget).closest('.source-line')
      highlighted = row.is ".#{HIGHLIGHTED_ROW_CLASS}"
      @$(".#{HIGHLIGHTED_ROW_CLASS}").removeClass HIGHLIGHTED_ROW_CLASS
      @highlightedLine = null
      unless highlighted
        row.addClass HIGHLIGHTED_ROW_CLASS
        @highlightedLine = row.data 'line-number'
        @showLineActionsPopup(e)


    highlightCurrentLine: ->
      if @highlightedLine?
        @$("[data-line-number=#{@highlightedLine}]").addClass HIGHLIGHTED_ROW_CLASS


    highlightUsages: (e) ->
      highlighted = $(e.currentTarget).is '.highlighted'
      key = e.currentTarget.className.split(/\s+/)[0]
      @$('.sym.highlighted').removeClass 'highlighted'
      @$(".sym.#{key}").addClass 'highlighted' unless highlighted


    toggleSettings: ->
      @$('.settings-toggle button').toggleClass 'open'
      @$('.component-viewer-source-settings').toggleClass 'open'


    toggleMeasures: (e) ->
      row = $(e.currentTarget).closest '.component-viewer-header'
      row.toggleClass 'component-viewer-header-full'


    showCoveragePopup: (e) ->
      e.stopPropagation()
      $('body').click()
      line = $(e.currentTarget).closest('.source-line').data 'line-number'
      row = _.findWhere @options.main.source.get('formattedSource'), lineNumber: line
      $.get API_COVERAGE_TESTS, key: @options.main.component.get('key'), line: line, (data) =>
        popup = new CoveragePopupView
          model: new Backbone.Model data
          triggerEl: $(e.currentTarget)
          main: @options.main
          row: row
        popup.render()


    showDuplicationPopup: (e) ->
      e.stopPropagation()
      $('body').click()
      index = $(e.currentTarget).data 'index'
      line = $(e.currentTarget).closest('[data-line-number]').data 'line-number'
      blocks = @model.get('duplications')[index - 1].blocks
      blocks = _.filter blocks, (b) ->
        (b._ref != '1') || (b._ref == '1' && b.from > line) || (b._ref == '1' && b.from + b.size < line)
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
        if line <= LINES_LIMIT
          @showBlocks.forEach (block) ->
            show = true if block.from <= line && block.to >= line
          _.extend sourceLine, show: show
      source


    prepareSource: ->
      source = @model.get 'formattedSource'
      if source?
        _.first @augmentWithShow(source), LINES_LIMIT


    getStatColumnsCount: ->
      count = 1 # line number
      count += 2 if @options.main.settings.get 'coverage'
      count += 1 if @options.main.settings.get 'duplications'
      count += 1 if @options.main.settings.get 'issues'
      count


    showZeroLine: ->
      r = false
      r = true unless @options.main.state.get 'hasSource'
      @showBlocks.forEach (block) ->
        r = true if block.from <= 0
      r


    getBaseDuplications: ->
      source = @model.get 'formattedSource'
      baseDuplications = []
      if source? && source.length > 0 && _.first(source).duplications?
        baseDuplications = _.first(source).duplications
      baseDuplications


    serializeData: ->
      uid: @cid
      source: @prepareSource()
      settings: @options.main.settings.toJSON()
      state: @options.main.state.toJSON()
      showSettings: @showSettings
      component: @options.main.component.toJSON()
      columns: @getStatColumnsCount() + 1
      showZeroLine: @showZeroLine()
      issuesLimit: ISSUES_LIMIT
      issuesLimitReached: @model.get('activeIssues')?.length > ISSUES_LIMIT
      linesLimit: LINES_LIMIT
      linesLimitReached: _.size(@model.get 'source') > LINES_LIMIT
      baseDuplications: @getBaseDuplications()

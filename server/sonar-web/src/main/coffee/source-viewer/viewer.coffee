define [
  'backbone.marionette'
  'templates/source-viewer'
  'source-viewer/source'
  'issue/models/issue'
  'issue/collections/issues'
  'issue/issue-view'

  'source-viewer/popups/coverage-popup'
  'source-viewer/popups/duplication-popup'
  'source-viewer/popups/line-actions-popup'
], (
  Marionette
  Templates
  Source
  Issue
  Issues
  IssueView

  CoveragePopupView
  DuplicationPopupView
  LineActionsPopupView
) ->

  $ = jQuery

  HIGHLIGHTED_ROW_CLASS = 'source-line-highlighted'

  log = (message) ->
    console.log 'Source Viewer:', message


  class extends Marionette.ItemView
    className: 'source'
    template: Templates['source-viewer']

    ISSUES_LIMIT: 100
    LINES_LIMIT: 1000
    LINES_AROUND: 500


    ui:
      sourceBeforeSpinner: '.js-component-viewer-source-before'
      sourceAfterSpinner: '.js-component-viewer-source-after'


    events: ->
      'click .source-line-covered': 'showCoveragePopup'
      'click .source-line-partially-covered': 'showCoveragePopup'

      'click .source-line-duplications': 'showDuplications'
      'click .source-line-duplications-extra': 'showDuplicationPopup'

      'click .source-line-number[data-line-number]': 'highlightLine'


    initialize: ->
      @model = new Source() unless @model?
      @issues = new Issues()
      @issueViews = []
      @loadSourceBeforeThrottled = _.throttle @loadSourceBefore, 1000
      @loadSourceAfterThrottled = _.throttle @loadSourceAfter, 1000
      @scrollTimer = null


    onRender: ->
      log 'Render'
      @renderIssues()


    onClose: ->
      @issueViews.forEach (view) -> view.close()
      @issueViews = []


    open: (id, key) ->
      @model.clear()
      @model.set uuid: id, key: key
      @requestComponent().done =>
        @requestSource()
        .done =>
          @requestCoverage().done =>
            @requestDuplications().done =>
              @requestIssues().done =>
                @render()
                @trigger 'loaded'
        .fail =>
          @model.set source: [{ line: 0 }]
          @requestIssues().done =>
            @render()
            @trigger 'loaded'


    requestComponent: ->
      log 'Request component details...'
      url = "#{baseUrl}/api/components/app"
      options = key: @model.key()
      $.get url, options, (data) =>
        @model.set data
        log 'Component loaded'


    linesLimit: ->
      from: 1
      to: @LINES_LIMIT


    requestSource: ->
      log 'Request source...'
      url = "#{baseUrl}/api/sources/lines"
      options = _.extend { uuid: @model.id }, @linesLimit()
      $.get url, options, (data) =>
        source = data.sources || []
        if source.length == 0 || (source.length > 0 && _.first(source).line == 1)
          source.unshift { line: 0 }
        firstLine = _.first(source).line
        @model.set
          source: source
          hasSourceBefore: firstLine > 1
          hasSourceAfter: true
        log 'Source loaded'


    requestCoverage: ->
      log 'Request coverage'
      url = "#{baseUrl}/api/coverage/show"
      options = key: @model.key()
      $.get url, options, (data) =>
        hasCoverage = data? && data.coverage?
        @model.set hasCoverage: hasCoverage
        if hasCoverage
          coverage = data.coverage.map (c) ->
            status = 'partially-covered'
            status = 'covered' if c[1] && c[3] == c[4]
            status = 'uncovered' if !c[1] || c[4] == 0
            line: +c[0]
            covered: status
        else coverage = []
        @model.addMeta coverage
        log 'Coverage loaded'


    requestDuplications: ->
      log 'Request duplications'
      url = "#{baseUrl}/api/duplications/show"
      options = key: @model.key()
      $.get url, options, (data) =>
        hasDuplications = data? && data.duplications?
        if hasDuplications
          duplications = {}
          data.duplications.forEach (d, i) ->
            d.blocks.forEach (b) ->
              if b._ref == '1'
                lineFrom = b.from
                lineTo = b.from + b.size
                duplications[i] = true for i in [lineFrom..lineTo]
          duplications = _.pairs(duplications).map (line) ->
            line: +line[0]
            duplicated: line[1]
        else duplications = []
        @model.addMeta duplications
        @model.addDuplications data.duplications
        @model.set
          duplications: data.duplications
          duplicationFiles: data.files
        log 'Duplications loaded'


    requestIssues: ->
      log 'Request issues'
      options = data:
        componentUuids: @model.id
        extra_fields: 'actions,transitions,assigneeName,actionPlanName'
        resolved: false
        s: 'FILE_LINE'
        asc: true
      @issues.fetch(options).done =>
        @issues.reset @limitIssues @issues
        @addIssuesPerLineMeta @issues
        log 'Issues loaded'


    addIssuesPerLineMeta: (issues) ->
      lines = {}
      issues.forEach (issue) ->
        line = issue.get('line') || 0
        lines[line] = [] unless _.isArray lines[line]
        lines[line].push issue.toJSON()
      issuesPerLine = _.pairs(lines).map (line) ->
        line: +line[0]
        issues: line[1]
      @model.addMeta issuesPerLine


    limitIssues: (issues) ->
      issues.first @ISSUES_LIMIT


    renderIssues: ->
      log 'Render issues'
      @issues.forEach @renderIssue, @
      log 'Issues rendered'


    renderIssue: (issue) ->
      issueView = new IssueView
        el: '#issue-' + issue.get('key')
        model: issue
      @issueViews.push issueView
      issueView.render()


    addIssue: (issue) ->
      line = issue.get('line') || 0
      code = @$(".source-line-code[data-line-number=#{line}]")
      issueList = code.find('.issue-list')
      unless issueList.length > 0
        issueList = $('<div class="issue-list"></div>')
        code.append issueList
      issueList.append "<div class=\"issue\" id=\"issue-#{issue.id}\"></div>"
      @renderIssue issue


    showSpinner: ->
    hideSpinner: ->
    resetShowBlocks: ->


    showCoveragePopup: (e) ->
      r = window.process.addBackgroundProcess()
      e.stopPropagation()
      $('body').click()
      line = $(e.currentTarget).data 'line-number'
      url = "#{baseUrl}/api/tests/test_cases"
      options =
        key: @model.key()
        line: line
      $.get url, options
      .done (data) =>
        popup = new CoveragePopupView
          model: new Backbone.Model data
          triggerEl: $(e.currentTarget)
        popup.render()
        window.process.finishBackgroundProcess r
      .fail ->
        window.process.failBackgroundProcess r


    showDuplications: ->
      @$('.source-line-duplications').addClass 'hidden'
      @$('.source-line-duplications-extra').removeClass 'hidden'


    showDuplicationPopup: (e) ->
      e.stopPropagation()
      $('body').click()
      index = $(e.currentTarget).data 'index'
      line = $(e.currentTarget).data 'line-number'
      blocks = @model.get('duplications')[index - 1].blocks
      blocks = _.filter blocks, (b) ->
        (b._ref != '1') || (b._ref == '1' && b.from > line) || (b._ref == '1' && b.from + b.size < line)
      popup = new DuplicationPopupView
        triggerEl: $(e.currentTarget)
        model: @model
        collection: new Backbone.Collection blocks
      popup.render()


    showLineActionsPopup: (e) ->
      e.stopPropagation()
      $('body').click()
      line = $(e.currentTarget).data 'line-number'
      popup = new LineActionsPopupView
        triggerEl: $(e.currentTarget)
        model: @model
        line: line
        row: $(e.currentTarget).closest '.source-line'
      popup.on 'onManualIssueAdded', (data) =>
        @addIssue new Issue(data)
      popup.render()


    highlightLine: (e) ->
      row = $(e.currentTarget).closest('.source-line')
      highlighted = row.is ".#{HIGHLIGHTED_ROW_CLASS}"
      @$(".#{HIGHLIGHTED_ROW_CLASS}").removeClass HIGHLIGHTED_ROW_CLASS
      unless highlighted
        row.addClass HIGHLIGHTED_ROW_CLASS
        @showLineActionsPopup(e)


    bindScrollEvents: ->
      @$el.scrollParent().on 'scroll.source-viewer', (=> @onScroll())


    unbindScrollEvents: ->
      @$el.scrollParent().off 'scroll.source-viewer'


    disablePointerEvents: ->
      clearTimeout @scrollTimer
      $('body').addClass 'disabled-pointer-events'
      @scrollTimer = setTimeout (-> $('body').removeClass 'disabled-pointer-events'), 250


    onScroll: ->
      @disablePointerEvents()

      p = @$el.scrollParent()
      p = $(window) if p.is(document)
      pTopOffset = if p.offset()? then p.offset().top else 0
      if @model.get('hasSourceBefore') && (p.scrollTop() + pTopOffset <= @ui.sourceBeforeSpinner.offset().top)
        @loadSourceBeforeThrottled()

      if @model.get('hasSourceAfter') && (p.scrollTop() + pTopOffset + p.height() >= @ui.sourceAfterSpinner.offset().top)
        @loadSourceAfterThrottled()


    loadSourceBefore: ->
      @unbindScrollEvents()
      source = @model.get 'source'
      firstLine = _.first(source).line
      url = "#{baseUrl}/api/sources/lines"
      options =
        uuid: @model.id
        from: firstLine - @LINES_AROUND
        to: firstLine - 1
      $.get url, options, (data) =>
        source = (data.sources || []).concat source
        if source.length == 0 || (source.length > 0 && _.first(source).line == 1)
          source.unshift { line: 0 }
        @model.set
          source: source
          hasSourceBefore: data.sources.length == @LINES_AROUND
        @render()
        @scrollToLine firstLine
        @bindScrollEvents() if @model.get('hasSourceBefore') || @model.get('hasSourceAfter')


    loadSourceAfter: ->
      @unbindScrollEvents()
      source = @model.get 'source'
      lastLine = _.last(source).line
      url = "#{baseUrl}/api/sources/lines"
      options =
        uuid: @model.id
        from: lastLine + 1
        to: lastLine + @LINES_AROUND
      $.get url, options
      .done (data) =>
        source = source.concat data.sources
        @model.set
          source: source
          hasSourceAfter: data.sources.length == @LINES_AROUND
        @render()
        @bindScrollEvents() if @model.get('hasSourceBefore') || @model.get('hasSourceAfter')
      .fail =>
        @model.set hasSourceAfter: false
        @render()
        @bindScrollEvents() if @model.get('hasSourceBefore') || @model.get('hasSourceAfter')

define [
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/coverage-popup'
  'component-viewer/duplication-popup'
  'issues/issue-view'
  'issues/models/issue'
  'common/handlebars-extensions'
], (
  Marionette
  Templates
  CoveragePopupView
  DuplicationPopupView
  IssueView
  Issue
) ->

  $ = jQuery


  class SourceView extends Marionette.ItemView
    template: Templates['source']
    expandTemplate: Templates['code-expand']

    LINES_AROUND_ISSUE = 4
    EXPAND_LINES = 20


    events:
      'click .js-toggle-settings': 'toggleSettings'
      'click .js-toggle-measures': 'toggleMeasures'
      'change #source-issues': 'toggleIssues'
      'change #source-coverage': 'toggleCoverage'
      'change #source-duplications': 'toggleDuplications'
      'change #source-workspace': 'toggleWorkspace'
      'click .coverage-tests': 'showCoveragePopup'

      'click .duplication-exists': 'showDuplicationPopup'
      'mouseenter .duplication-exists': 'duplicationMouseEnter'
      'mouseleave .duplication-exists': 'duplicationMouseLeave'

      'click .js-expand': 'expandBlock'
      'click .js-expand-all': 'expandAll'


    initialize: ->
      super
      @showBlocks = []


    onRender: ->
      @delegateEvents()
      @showSettings = false
      @renderExpandButtons()
      @renderIssues() if @options.main.settings.get('issues') && @model.has('issues')


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
      issues = @model.get 'issues'
      issues.forEach (issue) =>
        line = issue.line || 0
        row = @$("[data-line-number=#{line}]")
        row.removeClass 'row-hidden'
        container = row.children('.line')
        container.addClass 'issue' if line > 0
        issueView = new IssueView model: new Issue issue
        issueView.render().$el.appendTo container


    showSpinner: ->
      @$el.html '<div style="padding: 10px;"><i class="spinner"></i></div>'


    toggleSettings: ->
      @$('.settings-toggle button').toggleClass 'open'
      @$('.component-viewer-source-settings').toggleClass 'open'


    toggleMeasures: ->
      @$('.component-viewer-measures-section').toggleClass 'brief'


    toggleSetting: (e, show, hide) ->
      @showBlocks = []
      active = $(e.currentTarget).is ':checked'
      @showSettings = true
      if active then show.call @options.main else hide.call @options.main


    toggleIssues: (e) ->
      @toggleSetting e, @options.main.showIssues, @options.main.hideIssues


    toggleCoverage: (e) ->
      @toggleSetting e, @options.main.showCoverage, @options.main.hideCoverage


    toggleDuplications: (e) ->
      @toggleSetting e, @options.main.showDuplications, @options.main.hideDuplications


    toggleWorkspace: (e) ->
      @toggleSetting e, @options.main.showWorkspace, @options.main.hideWorkspace


    showCoveragePopup: (e) ->
      e.stopPropagation()
      $('body').click()
      popup = new CoveragePopupView
        triggerEl: $(e.currentTarget)
        main: @options.main
      popup.render()


    showDuplicationPopup: (e) ->
      e.stopPropagation()
      $('body').click()
      popup = new DuplicationPopupView
        triggerEl: $(e.currentTarget)
        main: @options.main
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
      @showBlocks.push from: 0, to: _.size @model.get 'source'
      @render()


    getLineCoverage: (line) ->
      coverage = @model.get 'coverage'

      lineCoverage = coverage? && coverage[line]? && coverage[line]
      lineCoverage = +lineCoverage if _.isString lineCoverage

      lineCoverageStatus = null
      if _.isNumber lineCoverage
        lineCoverageStatus = 'red' if lineCoverage == 0
        lineCoverageStatus = 'green' if lineCoverage > 0

      coverage: lineCoverage
      coverageStatus: lineCoverageStatus


    getLineCoverageConditions: (line) ->
      coverageConditions = @model.get 'coverageConditions'
      conditions = @model.get 'conditions'

      lineCoverageConditions = coverageConditions? && coverageConditions[line]? && coverageConditions[line]
      lineCoverageConditions = +lineCoverageConditions if _.isString lineCoverageConditions
      lineConditions = conditions? && conditions[line]? && conditions[line]
      lineConditions = +lineConditions if _.isString lineConditions

      lineCoverageConditionsStatus = null
      if _.isNumber(lineCoverageConditions) && _.isNumber(conditions)
        lineCoverageConditionsStatus = 'red' if lineCoverageConditions == 0
        lineCoverageConditionsStatus = 'orange' if lineCoverageConditions > 0 && lineCoverageConditions < lineConditions
        lineCoverageConditionsStatus = 'green' if lineCoverageConditions == lineConditions

      coverageConditions: lineCoverageConditions
      conditions: lineConditions
      coverageConditionsStatus: lineCoverageConditionsStatus


    getLineDuplications: (line) ->
      duplications = @model.get('duplications') || []
      lineDuplications = duplications.map (d) ->
        d.from <= line && (d.from + d.count) > line

      duplications: lineDuplications


    augmentWithShow: (sourceLine) ->
      show = false
      line = sourceLine.lineNumber

      @showBlocks.forEach (block) ->
        if block.from <= line && block.to >= line
          show = true

      if @options.main.settings.get('issues') && !show
        @model.get('issues')?.forEach (issue) ->
          if issue.line?
            if (issue.line - LINES_AROUND_ISSUE) <= line && (issue.line + LINES_AROUND_ISSUE) >= line
              show = true

      if @options.main.settings.get('coverage') && !show
        show = true if sourceLine.coverageStatus

      if @options.main.settings.get('duplications') && !show
        sourceLine.duplications.forEach (d) ->
          show = true if d

      _.extend sourceLine, show: show


    prepareSource: ->
      source = @model.get 'source'
      _.map source, (code, line) =>
        base = lineNumber: line, code: code
        if @options.main.settings.get('coverage')
          _.extend base, @getLineCoverage(line), @getLineCoverageConditions(line)
        if @options.main.settings.get('duplications')
          _.extend base, @getLineDuplications(line)
        @augmentWithShow base



    serializeData: ->
      source: @prepareSource()
      settings: @options.main.settings.toJSON()
      showSettings: @showSettings
      component: @options.main.component.toJSON()
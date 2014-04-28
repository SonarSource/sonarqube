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


    events:
      'click .js-toggle-settings': 'toggleSettings'
      'click .js-toggle-measures': 'toggleMeasures'
      'change #source-coverage': 'toggleCoverage'
      'change #source-duplications': 'toggleDuplications'
      'change #source-workspace': 'toggleWorkspace'
      'click .coverage-tests': 'showCoveragePopup'

      'click .duplication-exists': 'showDuplicationPopup'
      'mouseenter .duplication-exists': 'duplicationMouseEnter'
      'mouseleave .duplication-exists': 'duplicationMouseLeave'


    onRender: ->
      @delegateEvents()
      @showSettings = false

      @renderIssues() if @options.main.settings.get('issues') && @model.has('issues')


    renderIssues: ->
      issues = @model.get 'issues'
      issues.forEach (issue) =>
        line = issue.line || 0
        container = @$("[data-line-number=#{line}]").children('.line')
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


    toggleCoverage: (e) ->
      active = $(e.currentTarget).is ':checked'
      @showSettings = true
      if active then @options.main.showCoverage() else @options.main.hideCoverage()


    toggleDuplications: (e) ->
      active = $(e.currentTarget).is ':checked'
      @showSettings = true
      if active then @options.main.showDuplications() else @options.main.hideDuplications()


    toggleWorkspace: (e) ->
      active = $(e.currentTarget).is ':checked'
      @showSettings = true
      if active then @options.main.showWorkspace() else @options.main.hideWorkspace()


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


    prepareSource: ->
      source = @model.get 'source'
      coverage = @model.get 'coverage'
      coverageConditions = @model.get 'coverageConditions'
      conditions = @model.get 'conditions'
      duplications = @model.get('duplications') || []
      _.map source, (code, line) ->
        lineCoverage = coverage? && coverage[line]? && coverage[line]
        lineCoverage = +lineCoverage if _.isString lineCoverage
        lineCoverageConditions = coverageConditions? && coverageConditions[line]? && coverageConditions[line]
        lineCoverageConditions = +lineCoverageConditions if _.isString lineCoverageConditions
        lineConditions = conditions? && conditions[line]? && conditions[line]
        lineConditions = +lineConditions if _.isString lineConditions

        lineCoverageStatus = null
        if _.isNumber lineCoverage
          lineCoverageStatus = 'red' if lineCoverage == 0
          lineCoverageStatus = 'green' if lineCoverage > 0

        lineCoverageConditionsStatus = null
        if _.isNumber(lineCoverageConditions) && _.isNumber(conditions)
          lineCoverageConditionsStatus = 'red' if lineCoverageConditions == 0
          lineCoverageConditionsStatus = 'orange' if lineCoverageConditions > 0 && lineCoverageConditions < lineConditions
          lineCoverageConditionsStatus = 'green' if lineCoverageConditions == lineConditions

        lineDuplications = duplications.map (d) ->
          d.from <= line && (d.from + d.count) > line

        lineNumber: line
        code: code
        coverage: lineCoverage
        coverageStatus: lineCoverageStatus
        coverageConditions: lineCoverageConditions
        conditions: lineConditions
        coverageConditionsStatus: lineCoverageConditionsStatus || lineCoverageStatus
        duplications: lineDuplications


    serializeData: ->
      source: @prepareSource()
      settings: @options.main.settings.toJSON()
      showSettings: @showSettings
      component: @options.main.component.toJSON()
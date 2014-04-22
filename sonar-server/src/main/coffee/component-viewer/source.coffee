define [
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/coverage-popup'
  'common/handlebars-extensions'
], (
  Marionette
  Templates
  CoveragePopupView
) ->

  $ = jQuery


  class SourceView extends Marionette.ItemView
    template: Templates['source']


    events:
      'click .settings-toggle button': 'toggleSettings'
      'change #source-coverage': 'toggleCoverage'
      'click .coverage-tests': 'showCoveragePopup'


    onRender: ->
      @delegateEvents()
      @showSettings = false


    showSpinner: ->
      @$el.html '<div style="padding: 10px;"><i class="spinner"></i></div>'



    toggleSettings: ->
      @$('.settings-toggle button').toggleClass 'open'
      @$('.component-viewer-source-settings').toggleClass 'open'


    toggleCoverage: (e) ->
      active = $(e.currentTarget).is ':checked'
      @showSettings = true
      if active then @options.main.showCoverage() else @options.main.hideCoverage()


    showCoveragePopup: (e) ->
      e.stopPropagation()
      $('body').click()
      popup = new CoveragePopupView
        triggerEl: $(e.currentTarget)
        main: @options.main
      popup.render()


    prepareSource: ->
      source = @model.get 'source'
      coverage = @model.get 'coverage'
      coverageConditions = @model.get 'coverageConditions'
      conditions = @model.get 'conditions'
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

        lineNumber: line
        code: code
        coverage: lineCoverage
        coverageStatus: lineCoverageStatus
        coverageConditions: lineCoverageConditions
        conditions: lineConditions
        coverageConditionsStatus: lineCoverageConditionsStatus || lineCoverageStatus


    serializeData: ->
      source: @prepareSource()
      settings: @options.main.settings.toJSON()
      showSettings: @showSettings
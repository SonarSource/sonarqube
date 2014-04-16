define [
  'backbone.marionette'
  'templates/component-viewer'
  'common/handlebars-extensions'
], (
  Marionette
  Templates
) ->

  class SourceView extends Marionette.ItemView
    template: Templates['source']


    modelEvents:
      'change': 'render'


    serializeData: ->
      source = @model.get 'source'
      coverage = @model.get 'coverage'
      coverageConditions = @model.get 'coverageConditions'
      conditions = @model.get 'conditions'
      source = _.map source, (code, line) ->
        lineCoverage = coverage? && coverage[line]? && coverage[line]
        lineCoverageConditions = coverageConditions? && coverageConditions[line]? && coverageConditions[line]
        lineConditions = conditions? && conditions[line]? && conditions[line]
        lineCoverageStatus = lineCoverage? &&  if lineCoverage > 0 then 'green' else 'red'
        lineCoverageConditionsStatus = null
        if lineCoverageConditions? && conditions?
          lineCoverageConditionsStatus = 'red' if lineCoverageConditions == 0
          lineCoverageConditionsStatus = 'orange' if lineCoverageConditions > 0 && lineCoverageConditions < lineConditions
          lineCoverageConditionsStatus = 'green' if lineCoverageConditions == lineConditions


        lineNumber: line
        code: code
        coverage: lineCoverage
        coverageStatus: lineCoverageStatus
        coverageConditions: lineCoverageConditions
        conditions: lineConditions
        coverageConditionsStatus: lineCoverageConditionsStatus
      source: source
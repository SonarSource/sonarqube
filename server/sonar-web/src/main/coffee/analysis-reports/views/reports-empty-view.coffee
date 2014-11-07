define [
  'backbone.marionette'
  'templates/analysis-reports'
], (
  Marionette
  Templates
) ->

  class extends Marionette.ItemView
    className: 'analysis-reports-no-results'
    tagName: 'li'
    template: Templates['analysis-reports-empty']

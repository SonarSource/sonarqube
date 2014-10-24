define [
  'backbone.marionette'
  'templates/analysis-reports'
], (
  Marionette
  Templates
) ->

  class extends Marionette.ItemView
    tagName: 'li'
    template: Templates['analysis-reports-empty']

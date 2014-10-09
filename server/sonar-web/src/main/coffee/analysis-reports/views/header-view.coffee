define [
  'backbone.marionette'
  'templates/analysis-reports'
], (
  Marionette
  Templates
) ->

  class extends Marionette.ItemView
    template: Templates['analysis-reports-header']

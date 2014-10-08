define [
  'backbone.marionette'
  'templates/monitoring'
], (
  Marionette
  Templates
) ->

  class extends Marionette.ItemView
    tagName: 'li'
    template: Templates['monitoring-report']

define [
  'backbone.marionette'
  'templates/monitoring'
], (
  Marionette
  Templates
) ->

  class extends Marionette.ItemView
    template: Templates['monitoring-header']

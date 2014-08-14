define [
  'backbone.marionette',
  'templates/design'
], (
  Marionette,
  Templates
) ->


  class extends Marionette.ItemView
    template: Templates['dsm-info']

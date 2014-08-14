define [
  'backbone.marionette',
  'templates/design'
], (
  Marionette,
  Templates
) ->


  class extends Marionette.ItemView
    template: Templates['dsm-info']


    serializeData: ->
      _.extend super,
        first: @options.first
        second: @options.second

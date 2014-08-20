define [
  'backbone.marionette'
  'templates/libraries'
], (
  Marionette,
  Templates
) ->

  $ = jQuery



  class extends Marionette.ItemView
    template: Templates['libraries']

define [
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/popup'
], (
  Marionette
  Templates
  Popup
) ->

  $ = jQuery


  class extends Popup
    template: Templates['extensions-popup']


    events:
      'click .js-extension': 'showExtension'


    showExtension: (e) ->
      e.preventDefault()
      key = $(e.currentTarget).data 'key'
      @trigger 'extension', key


    serializeData: ->
      _.extend super,
        extensions: @options.main.state.get 'extensions'

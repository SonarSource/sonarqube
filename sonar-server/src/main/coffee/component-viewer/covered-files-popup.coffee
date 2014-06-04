define [
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/popup'
  'component-viewer/utils'
], (
  Marionette
  Templates
  Popup
  utils
) ->

  $ = jQuery


  class CoveredFilesPopupView extends Popup
    template: Templates['covered-files-popup']


    events:
      'click a[data-key]': 'goToFile'


    goToFile: (e) ->
      key = $(e.currentTarget).data 'key'
      files = @collection.toJSON()
      @options.main.addTransition 'covers', _.map files, (file) ->
        x = utils.splitLongName file.longName
        key: file.key
        name: x.name
        subname: x.dir
        active: file.key == key
      @options.main._open key


    serializeData: ->
      items: @collection.toJSON().map (file) ->
        _.extend file, utils.splitLongName file.longName
      test: @options.test

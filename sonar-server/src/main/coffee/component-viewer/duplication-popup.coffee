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


  class DuplicationPopupView extends Popup
    template: Templates['duplicationPopup']


    events:
      'click a[data-key]': 'goToFile'


    goToFile: (e) ->
      key = $(e.currentTarget).data 'key'
      files = @options.main.source.get('duplicationFiles')
      @options.main.addTransition key, 'duplication', @collection.map (item) ->
        file = files[item.get('_ref')]
        x = utils.splitLongName file.name
        key: file.key
        name: x.name
        subname: x.dir
        active: file.key == key


    serializeData: ->
      files = @options.main.source.get('duplicationFiles')
      blocks = _.groupBy _.rest(@collection.toJSON()), '_ref'
      duplications = _.map blocks, (blocks, fileRef) ->
        blocks: blocks
        file: files[fileRef]
      duplications: duplications

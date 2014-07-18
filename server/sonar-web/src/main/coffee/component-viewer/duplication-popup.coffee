define [
  'backbone.marionette'
  'templates/component-viewer'
  'common/popup'
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
      line = $(e.currentTarget).data 'line'
      if key == @options.main.component.get 'key'
        return @options.main.scrollToLine line
      files = @options.main.source.get('duplicationFiles')
      @options.main.addTransition 'duplication', @collection.map (item) ->
        file = files[item.get('_ref')]
        x = utils.splitLongName file.name
        key: file.key
        name: x.name
        subname: x.dir
        active: file.key == key
      @options.main._open key
      @options.main.on 'sized', =>
        @options.main.off 'sized'
        @options.main.scrollToLine line


    serializeData: ->
      files = @options.main.source.get('duplicationFiles')
      groupedBlocks = _.groupBy @collection.toJSON(), '_ref'
      duplications = _.map groupedBlocks, (blocks, fileRef) ->
        blocks: blocks
        file: files[fileRef]
      duplications = _.sortBy duplications, (d) =>
        d.file.projectName != @options.main.component.get 'projectName'
      component: @options.main.component.toJSON()
      duplications: duplications

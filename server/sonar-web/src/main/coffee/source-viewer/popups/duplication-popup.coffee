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


  class extends Popup
    template: Templates['source-viewer-duplication-popup']


    events:
      'click a[data-key]': 'goToFile'


    goToFile: (e) ->
      key = $(e.currentTarget).data 'key'
      line = $(e.currentTarget).data 'line'
      files = @options.main.source.get('duplicationFiles')
      options =  @collection.map (item) ->
        file = files[item.get('_ref')]
        x = utils.splitLongName file.name
        key: file.key
        name: x.name
        subname: x.dir
        component:
          projectName: file.projectName
          subProjectName: file.subProjectName
        active: file.key == key
      options = _.uniq options, (item) -> item.key


    serializeData: ->
      files = @model.get 'duplicationFiles'
      groupedBlocks = _.groupBy @collection.toJSON(), '_ref'
      duplications = _.map groupedBlocks, (blocks, fileRef) ->
        blocks: blocks
        file: files[fileRef]

      duplications = _.sortBy duplications, (d) =>
        a = d.file.projectName != @model.get 'projectName'
        b = d.file.subProjectName != @model.get 'subProjectName'
        c = d.file.key != @model.get 'key'
        '' + a + b + c

      component: @model.toJSON()
      duplications: duplications

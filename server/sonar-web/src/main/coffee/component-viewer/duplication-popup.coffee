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
    template: Templates['cw-duplication-popup']


    events:
      'click a[data-key]': 'goToFile'


    onRender: ->
      source = @options.main.sourceView.$el
      sourceOffset = source.offset()
      trigger = @options.triggerEl
      triggerOffset = trigger.offset()
      @$el.detach().appendTo(source).css
        top: triggerOffset.top - sourceOffset.top + source.scrollTop()
        left: triggerOffset.left - sourceOffset.left + source.scrollLeft() + trigger.outerWidth()
      @attachCloseEvents()


    goToFile: (e) ->
      key = $(e.currentTarget).data 'key'
      line = $(e.currentTarget).data 'line'
      files = @options.main.source.get('duplicationFiles')
      @options.main.addTransition 'duplication', @collection.map (item) ->
        file = files[item.get('_ref')]
        x = utils.splitLongName file.name
        key: file.key
        name: x.name
        subname: x.dir
        component:
          projectName: file.projectName
          subProjectName: file.subProjectName
        active: file.key == key
      if key == @options.main.component.get 'key'
        @options.main.scrollToLine line
        @options.main.workspaceView.render()
      else
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
        a = d.file.projectName != @options.main.component.get 'projectName'
        b = d.file.subProjectName != @options.main.component.get 'subProjectName'
        c = d.file.key != @options.main.component.get 'key'
        '' + a + b + c

      component: @options.main.component.toJSON()
      duplications: duplications

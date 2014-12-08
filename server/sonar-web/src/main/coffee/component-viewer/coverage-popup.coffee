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


  class CoveragePopupView extends Popup
    template: Templates['cw-coverage-popup']


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
      el = $(e.currentTarget)
      key = el.data 'key'
      method = el.data 'method'
      files = @model.get 'files'
      @options.main.addTransition 'coverage', _.map files, (file) ->
        x = utils.splitLongName file.longName
        key: file.key
        name: x.name
        subname: x.dir
        active: file.key == key
      @options.main.state.unset 'activeHeaderTab'
      @options.main.state.unset 'activeHeaderItem'
      @options.main._open key
      @options.main.on 'loaded', =>
        @options.main.off 'loaded'
        @options.main.headerView.enableBar('tests').done =>
          if method?
            @options.main.headerView.enableUnitTest method


    serializeData: ->
      files = @model.get 'files'
      tests = _.groupBy @model.get('tests'), '_ref'
      testFiles = _.map tests, (testSet, fileRef) ->
        file: files[fileRef]
        tests: testSet
      testFiles: testFiles
      row: @options.row

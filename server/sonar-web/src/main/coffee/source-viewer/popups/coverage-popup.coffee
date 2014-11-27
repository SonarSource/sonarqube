define [
  'backbone.marionette'
  'templates/source-viewer'
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
    template: Templates['source-viewer-coverage-popup']


    events:
      'click a[data-key]': 'goToFile'


    onRender: ->
      super
      @$('.bubble-popup-container').isolatedScroll()


    goToFile: (e) ->
      el = $(e.currentTarget)
      key = el.data 'key'
      method = el.data 'method'
      files = @model.get 'files'


    serializeData: ->
      files = @model.get 'files'
      tests = _.groupBy @model.get('tests'), '_ref'
      testFiles = _.map tests, (testSet, fileRef) ->
        file: files[fileRef]
        tests: testSet
      testFiles: testFiles

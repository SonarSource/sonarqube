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


  class CoveragePopupView extends Popup
    template: Templates['coveragePopup']


    events:
      'click a[data-key]': 'goToFile'


    goToFile: (e) ->
      key = $(e.currentTarget).data 'key'
      @options.main.addTransition key, 'coverage'


    serializeData: ->
      files = @model.get 'files'
      tests = _.groupBy @model.get('tests'), '_ref'
      testFiles = _.map tests, (testSet, fileRef) ->
        file: files[fileRef]
        tests: testSet
      testFiles: testFiles
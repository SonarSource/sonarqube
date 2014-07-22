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

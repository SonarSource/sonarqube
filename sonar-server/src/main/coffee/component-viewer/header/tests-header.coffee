define [
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/header/base-header'
  'component-viewer/covered-files-popup'
], (
  Marionette
  Templates
  BaseHeaderView
  CoveredFilesPopupView
) ->

  $ = jQuery
  API_TESTS_COVERED_FILES = "#{baseUrl}/api/tests/covered_files"


  class extends BaseHeaderView
    template: Templates['tests-header']


    ui:
      unitTests: '.js-unit-test'


    events:
      'click @ui.unitTests': 'showCoveredFiles'


    onRender: ->
      @header.enableUnitTest = (testName) =>
        @ui.unitTests.filter("[data-name=#{testName}]").click()


    onClose: ->
      delete @header.enableUnitTest


    showCoveredFiles: (e) ->
      e.stopPropagation()
      $('body').click()
      testName = $(e.currentTarget).data 'name'
      test = _.findWhere @component.get('tests'), name: testName
      key = @component.get('key')
      $.get API_TESTS_COVERED_FILES, key: key, test: testName, (data) =>
        popup = new CoveredFilesPopupView
          triggerEl: $(e.currentTarget)
          collection: new Backbone.Collection data.files
          test: test
          main: @options.main
        popup.render()
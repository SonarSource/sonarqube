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
    template: Templates['cw-tests-header']


    ui:
      unitTests: '.js-unit-test'


    events:
      'click @ui.unitTests': 'showCoveredFiles'
      'click .js-sort-tests-duration': 'sortTestsByDuration'
      'click .js-sort-tests-name': 'sortTestsByName'


    initialize: ->
      super
      @tests = _.sortBy @component.get('tests'), 'name'
      @activeSort = '.js-sort-tests-name'


    onRender: ->
      @header.enableUnitTest = (testName) =>
        test = @ui.unitTests.filter("[data-name=#{testName}]")
        container = test.closest '.component-viewer-header-expanded-bar-section-list'
        topOffset = test.offset().top - container.offset().top
        if topOffset > container.height()
          container.scrollTop topOffset
        test.click()
      @$(@activeSort).addClass 'active-link' if @activeSort


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
          main: @main
        popup.render()


    sortTestsByDuration: ->
      @activeSort = '.js-sort-tests-duration'
      @tests = _.sortBy @tests, 'durationInMs'
      @render()


    sortTestsByName: ->
      @activeSort = '.js-sort-tests-name'
      @tests = _.sortBy @tests, 'name'
      @render()


    hasCoveragePerTestData: ->
      hasData = false
      @component.get('tests').forEach (test) ->
        hasData = true if test.coveredLines
      hasData


    serializeData: ->
      _.extend super,
        tests: @tests
        hasCoveragePerTestData: @hasCoveragePerTestData()

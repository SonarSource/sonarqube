define [
  'backbone.marionette',
  'templates/coding-rules-old'
], (
  Marionette,
  Templates
) ->

  class AppLayout extends Marionette.Layout
    className: 'navigator coding-rules-navigator'
    template: Templates['coding-rules-layout']
    storageKey: 'codingRulesResultsWidth'


    regions:
      headerRegion: '.navigator-header'
      actionsRegion: '.navigator-actions'
      resultsRegion: '.navigator-results'
      detailsRegion: '.navigator-details'
      filtersRegion: '.navigator-filters'
      facetsRegion: '.navigator-facets'


    ui:
      side: '.navigator-side'
      results: '.navigator-results'
      details: '.navigator-details'
      resizer: '.navigator-resizer'


    initialize: ->
      jQuery(window).on 'resize', => @onResize()

      @isResize = false
      jQuery('body').on 'mousemove', (e) => @processResize(e)
      jQuery('body').on 'mouseup', => @stopResize()


    onRender: ->
      @ui.resizer.on 'mousedown', (e) => @startResize(e)

      resultsWidth = localStorage.getItem @storageKey
      if resultsWidth
        @$(@resultsRegion.el).width +resultsWidth
        @ui.side.width +resultsWidth + 20


    onResize: ->
      footerEl = jQuery('#footer')
      footerHeight = footerEl.outerHeight true

      resultsEl = @ui.results
      resultsHeight = jQuery(window).height() - resultsEl.offset().top -
        parseInt(resultsEl.css('margin-bottom'), 10) - footerHeight
      resultsEl.height resultsHeight

      detailsEl = @ui.details
      detailsWidth = jQuery(window).width() - detailsEl.offset().left -
        parseInt(detailsEl.css('margin-right'), 10)
      detailsHeight = jQuery(window).height() - detailsEl.offset().top -
        parseInt(detailsEl.css('margin-bottom'), 10) - footerHeight
      detailsEl.width(detailsWidth).height detailsHeight


    showSpinner: (region) ->
      @[region].show new Marionette.ItemView
        template: _.template('<i class="spinner"></i>')


    startResize: (e) ->
      @isResize = true
      @originalWidth = @ui.results.width()
      @x = e.clientX
      jQuery('html').attr('unselectable', 'on').css('user-select', 'none').on('selectstart', false)


    processResize: (e) ->
      if @isResize
        delta = e.clientX - @x
        @$(@resultsRegion.el).width @originalWidth + delta
        @ui.side.width @originalWidth + 20 + delta
        localStorage.setItem @storageKey, @ui.results.width()
        @onResize()


    stopResize: ->
      if @isResize
        jQuery('html').attr('unselectable', 'off').css('user-select', 'text').off('selectstart')
      @isResize = false
      true

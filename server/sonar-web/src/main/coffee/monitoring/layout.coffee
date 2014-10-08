define [
  'backbone.marionette',
  'templates/monitoring'
], (
  Marionette,
  Templates
) ->

  class extends Marionette.Layout
    className: 'navigator monitoring-navigator'
    template: Templates['monitoring-layout']


    regions:
      headerRegion: '.navigator-header'
      resultsRegion: '.navigator-results'


    ui:
      side: '.navigator-side'
      results: '.navigator-results'


    initialize: ->
      jQuery(window).on 'resize', => @onResize()


    onResize: ->
      footerEl = jQuery('#footer')
      footerHeight = footerEl.outerHeight true

      resultsEl = @ui.results
      resultsHeight = jQuery(window).height() - resultsEl.offset().top -
        parseInt(resultsEl.css('margin-bottom'), 10) - footerHeight
      resultsEl.height resultsHeight


    showSpinner: (region) ->
      @[region].show new Marionette.ItemView
        template: _.template('<i class="spinner"></i>')

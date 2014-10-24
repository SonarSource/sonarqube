define [
  'backbone.marionette',
  'templates/analysis-reports'
], (
  Marionette,
  Templates
) ->

  class extends Marionette.Layout
    className: 'navigator analysis-reports-navigator'
    template: Templates['analysis-reports-layout']


    regions:
      headerRegion: '.navigator-header'
      actionsRegion: '.navigator-actions'
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
      resultsHeight = jQuery(window).height() - resultsEl.offset().top - footerHeight
      resultsEl.height resultsHeight


    showSpinner: (region) ->
      @[region].show new Marionette.ItemView
        template: _.template('<i class="spinner"></i>')

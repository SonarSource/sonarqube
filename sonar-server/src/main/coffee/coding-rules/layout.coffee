define [
  'backbone.marionette',
  'templates/coding-rules'
], (
  Marionette,
  Templates
) ->

  class AppLayout extends Marionette.Layout
    className: 'navigator coding-rules-navigator'
    template: Templates['coding-rules-layout']
    spinner: '<i class="spinner"></i>'


    regions:
      headerRegion: '.navigator-header'
      actionsRegion: '.navigator-actions'
      resultsRegion: '.navigator-results'
      detailsRegion: '.navigator-details'
      filtersRegion: '.navigator-filters'
      facetsRegion: '.navigator-facets'


    initialize: ->
      jQuery(window).on 'resize', => @onResize()


    onResize: ->
      footerEl = jQuery('#footer')
      footerHeight = footerEl.outerHeight true

      resultsEl = jQuery('.navigator-results')
      resultsHeight = jQuery(window).height() - resultsEl.offset().top -
        parseInt(resultsEl.css('margin-bottom'), 10) - footerHeight
      resultsEl.height resultsHeight

      detailsEl = jQuery('.navigator-details')
      detailsWidth = jQuery(window).width() - detailsEl.offset().left -
        parseInt(detailsEl.css('margin-right'), 10)
      detailsHeight = jQuery(window).height() - detailsEl.offset().top -
        parseInt(detailsEl.css('margin-bottom'), 10) - footerHeight
      detailsEl.width(detailsWidth).height detailsHeight


    showSpinner: (region) ->
      @$(@[region].el).html @spinner

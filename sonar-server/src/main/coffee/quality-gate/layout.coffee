define [
  'backbone.marionette',
  'templates/quality-gates'
], (
  Marionette,
  Templates
) ->

  class AppLayout extends Marionette.Layout
    className: 'navigator quality-gates-navigator'
    template: Templates['quality-gates-layout']


    regions:
      headerRegion: '.navigator-header'
      actionsRegion: '.navigator-actions'
      resultsRegion: '.navigator-results'
      detailsRegion: '.navigator-details'


    initialize: (options) ->
      @listenTo options.app.qualityGates, 'all', @updateLayout
      jQuery(window).on 'resize', => @onResize()


    updateLayout: ->
      empty = @options.app.qualityGates.length == 0
      @$(@headerRegion.el).toggle !empty
      @$(@detailsRegion.el).toggle !empty


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


    onRender: ->
      @updateLayout()
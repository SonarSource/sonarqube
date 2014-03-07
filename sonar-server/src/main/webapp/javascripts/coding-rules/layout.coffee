define [
  'backbone.marionette',
  'common/handlebars-extensions'
], (
  Marionette
) ->

  class AppLayout extends Marionette.Layout
    className: 'navigator coding-rules-navigator'
    template: getTemplate '#coding-rules-layout'
    spinner: '<i class="spinner"></i>'


    regions:
      headerRegion: '.navigator-header'
      actionsRegion: '.navigator-actions'
      resultsRegion: '.navigator-results'
      detailsRegion: '.navigator-details'
      filtersRegion: '.navigator-filters'
      facetsRegion: '.navigator-facets'


    onRender: ->
      # Adjust details region height
      @$(@detailsRegion.el).css 'bottom', jQuery('#footer').outerHeight()


    showSpinner: (region) ->
      @$(@[region].el).html @spinner

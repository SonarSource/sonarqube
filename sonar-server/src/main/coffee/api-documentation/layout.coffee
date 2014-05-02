define [
  'backbone.marionette',
  'templates/api-documentation'
], (
  Marionette,
  Templates
) ->

  class AppLayout extends Marionette.Layout
    className: 'navigator quality-gates-navigator'
    template: Templates['api-documentation-layout']

    regions:
      resultsRegion: '.navigator-results'
      detailsRegion: '.navigator-details'

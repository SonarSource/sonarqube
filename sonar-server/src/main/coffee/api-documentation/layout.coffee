define [
  'backbone.marionette',
  'templates/api-documentation'
], (
  Marionette,
  Templates
) ->

  class AppLayout extends Marionette.Layout
    className: 'api-documentation-layout'
    template: Templates['api-documentation-layout']

    regions:
      resultsRegion: '.api-documentation-results'

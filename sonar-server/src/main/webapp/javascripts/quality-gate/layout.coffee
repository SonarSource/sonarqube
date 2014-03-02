define [
  'backbone.marionette',
  'common/handlebars-extensions'
], (
  Marionette
) ->

  class AppLayout extends Marionette.Layout
    className: 'navigator quality-gates-navigator'
    template: getTemplate '#quality-gates-layout'
    regions:
      headerRegion: '.navigator-header'
      actionsRegion: '.navigator-actions'
      listRegion: '.navigator-results'
      detailsRegion: '.navigator-details'

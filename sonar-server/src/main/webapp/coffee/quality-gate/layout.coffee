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


    updateLayout: ->
      empty = @options.app.qualityGates.length == 0
      @$(@headerRegion.el).toggle !empty
      @$(@detailsRegion.el).toggle !empty


    onRender: ->
      @updateLayout()

      # Adjust details region height
      @$(@detailsRegion.el).css 'bottom', jQuery('#footer').outerHeight()

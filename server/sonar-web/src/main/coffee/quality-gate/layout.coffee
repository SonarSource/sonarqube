define [
  'templates/quality-gates'
], ->

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


    onRender: ->
      @updateLayout()

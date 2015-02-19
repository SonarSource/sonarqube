define [
  'templates/api-documentation'
], ->

  $ = jQuery

  class extends Marionette.Layout
    template: Templates['api-documentation-layout']


    regions:
      resultsRegion: '.api-documentation-results'
      detailsRegion: '.search-navigator-workspace'


    events:
      'change #api-documentation-show-internals': 'toggleInternals'


    initialize: (app) ->
      @app = app.app
      @listenTo(@app.webServices, 'sync', @app.refresh)


    onRender: ->
      $('.search-navigator').addClass 'sticky'
      top = $('.search-navigator').offset().top
      @$('.search-navigator-side').css({ top: top }).isolatedScroll()


    toggleInternals: (event) ->
      @app.webServices.toggleInternals()

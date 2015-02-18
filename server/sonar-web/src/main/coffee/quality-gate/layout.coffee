define [
  'templates/quality-gates'
], ->

  $ = jQuery

  class AppLayout extends Marionette.Layout
    template: Templates['quality-gates-layout']


    regions:
      headerRegion: '.search-navigator-workspace-header'
      actionsRegion: '.search-navigator-filters'
      resultsRegion: '.quality-gates-results'
      detailsRegion: '.search-navigator-workspace-list'


    onRender: ->
      $('.search-navigator').addClass 'sticky'
      top = $('.search-navigator').offset().top
      @$('.search-navigator-workspace-header').css top: top
      @$('.search-navigator-side').css({ top: top }).isolatedScroll()

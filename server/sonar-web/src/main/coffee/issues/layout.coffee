define [
  'templates/issues'
], ->

  $ = jQuery


  class extends Marionette.Layout
    template: Templates['issues-layout']
    topOffset: 30


    regions:
      filtersRegion: '.search-navigator-filters'
      facetsRegion: '.search-navigator-facets'
      workspaceHeaderRegion: '.search-navigator-workspace-header'
      workspaceListRegion: '.search-navigator-workspace-list'
      workspaceComponentViewerRegion: '.issues-workspace-component-viewer'


    showComponentViewer: ->
      @scroll = $(window).scrollTop()
      $('.issues').addClass 'issues-extended-view'


    hideComponentViewer: ->
      $('.issues').removeClass 'issues-extended-view'
      $(window).scrollTop @scroll if @scroll?

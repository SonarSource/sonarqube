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


    initialize: ->
      $(window).on 'scroll.issues-layout', (=> @onScroll())


    onClose: ->
      $(window).off 'scroll.issues-layout'


    onScroll: ->
      scrollTop = $(window).scrollTop()
      $('.search-navigator').toggleClass 'sticky', scrollTop >= @topOffset
      @$('.search-navigator-side').css top: Math.max(0, Math.min(@topOffset - scrollTop, @topOffset))


    showSpinner: (region) ->
      @[region].show new Marionette.ItemView
        template: _.template('<i class="spinner"></i>')


    showComponentViewer: ->
      @scroll = $(window).scrollTop()
      $('.issues').addClass 'issues-extended-view'


    hideComponentViewer: ->
      $('.issues').removeClass 'issues-extended-view'
      $(window).scrollTop @scroll if @scroll?

define [
  'templates/issues'
], ->

  $ = jQuery


  class extends Marionette.Layout
    template: Templates['issues-layout']


    regions:
      filtersRegion: '.search-navigator-filters'
      facetsRegion: '.search-navigator-facets'
      workspaceHeaderRegion: '.search-navigator-workspace-header'
      workspaceListRegion: '.search-navigator-workspace-list'
      workspaceComponentViewerRegion: '.issues-workspace-component-viewer'


    initialize: ->
      @topOffset = 0
      $(window).on 'scroll.issues-layout', (=> @onScroll())


    onClose: ->
      $(window).off 'scroll.issues-layout'


    onRender: ->
      @$(@filtersRegion.el).addClass('hidden') if @options.app.state.get('isContext')
      top = $('.search-navigator').offset().top
      @topOffset = top
      @$('.search-navigator-side').css({ top: top }).isolatedScroll()


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

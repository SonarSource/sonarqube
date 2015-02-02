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


    onRender: ->
      @$(@filtersRegion.el).addClass('hidden') if @options.app.state.get('isContext')
      $('.search-navigator').addClass 'sticky'
      top = $('.search-navigator').offset().top
      @$('.search-navigator-workspace-header').css top: top
      @$('.search-navigator-side').css({ top: top }).isolatedScroll()


    showSpinner: (region) ->
      @[region].show new Marionette.ItemView
        template: _.template('<i class="spinner"></i>')


    showComponentViewer: ->
      @scroll = $(window).scrollTop()
      $('.issues').addClass 'issues-extended-view'


    hideComponentViewer: ->
      $('.issues').removeClass 'issues-extended-view'
      $(window).scrollTop @scroll if @scroll?

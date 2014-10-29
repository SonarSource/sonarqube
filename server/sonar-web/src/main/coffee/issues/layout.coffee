define [
  'backbone.marionette'
  'templates/issues'
], (
  Marionette
  Templates
) ->

  $ = jQuery

  # http://stackoverflow.com/questions/7600454/how-to-prevent-page-scrolling-when-scrolling-a-div-element
  $.fn.isolatedScroll = ->
    @on 'mousewheel DOMMouseScroll', (e) ->
      delta = e.wheelDelta || (e.originalEvent && e.originalEvent.wheelDelta) || -e.detail
      bottomOverflow = @scrollTop + $(@).outerHeight() - @scrollHeight >= 0
      topOverflow = @scrollTop <= 0
      e.preventDefault() if (delta < 0 && bottomOverflow) || (delta > 0 && topOverflow)
    @


  class extends Marionette.Layout
    template: Templates['issues-layout']


    regions:
      filtersRegion: '.issues-filters'
      facetsRegion: '.issues-facets'
      workspaceHeaderRegion: '.issues-workspace-header'
      workspaceListRegion: '.issues-workspace-list'
      workspaceComponentViewerRegion: '.issues-workspace-component-viewer'


    initialize: ->
      $(window).on 'scroll.issues-layout', (=> @onScroll())


    onClose: ->
      $(window).off 'scroll.issues-layout'


    onRender: ->
      @$('.issues-side').isolatedScroll()


    onScroll: ->
      scrollTop = $(window).scrollTop()
      $('.issues').toggleClass 'sticky', scrollTop >= 30
      @$('.issues-side').css top: Math.max(0, Math.min(30 - scrollTop, 30))


    showSpinner: (region) ->
      @[region].show new Marionette.ItemView
        template: _.template('<i class="spinner"></i>')


    showComponentViewer: ->
      @scroll = $(window).scrollTop()
      $('.issues').addClass 'issues-extended-view'


    hideComponentViewer: ->
      $('.issues').removeClass 'issues-extended-view'
      $(window).scrollTop @scroll if @scroll?

define [
  'backbone.marionette'
], (
  Marionette
) ->

  $ = jQuery

  $.fn.isolatedScroll = ->
    @on 'wheel', (e) ->
      delta = -e.originalEvent.deltaY
      bottomOverflow = @scrollTop + $(@).outerHeight() - @scrollHeight >= 0
      topOverflow = @scrollTop <= 0
      e.preventDefault() if (delta < 0 && bottomOverflow) || (delta > 0 && topOverflow)
    @


  class extends Marionette.ItemView
    className: 'overlay-popup'


    events: ->
      'click .overlay-popup-close': 'close'


    onRender: ->
      @$el.isolatedScroll()
      @$el.detach().appendTo $('body')
      key 'escape', 'overlay-popup', => @close()
      @keyScope = key.getScope()
      key.setScope 'overlay-popup'


    onClose: ->
      key.unbind 'overlay-popup'
      key.setScope @keyScope

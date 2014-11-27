define [
  'backbone.marionette'
], (
  Marionette
) ->

  $ = jQuery


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

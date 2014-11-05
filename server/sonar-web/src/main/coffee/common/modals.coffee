define [
  'backbone.marionette'
], (
  Marionette
) ->

  $ = jQuery
  EVENT_SCOPE = 'modal'


  class extends Marionette.ItemView
    className: 'modal'
    overlayClassName: 'modal-overlay'


    events:
      'click .js-modal-close': 'close'


    onRender: ->
      @$el.detach().appendTo $('body')
      @renderOverlay()
      key 'escape', (=> @close())


    onClose: ->
      @removeOverlay()


    renderOverlay: ->
      overlay = $(".#{@overlayClassName}")
      if overlay.length == 0
        overlay = $("<div class=\"#{@overlayClassName}\"></div>").appendTo $('body')


    removeOverlay: ->
      $('.modal-overlay').remove()


    attachCloseEvents: ->
      $('body').on "click.#{EVENT_SCOPE}", =>
        $('body').off "click.#{EVENT_SCOPE}"
        @close()

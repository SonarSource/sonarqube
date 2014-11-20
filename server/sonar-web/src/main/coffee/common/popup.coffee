define [
  'backbone.marionette'
], (
  Marionette
) ->

  $ = jQuery


  class PopupView extends Marionette.ItemView
    className: 'bubble-popup'


    onRender: ->
      @$el.detach().appendTo $('body')

      if @options.bottom
        @$el.addClass 'bubble-popup-bottom'
        @$el.css
          top: @options.triggerEl.offset().top + @options.triggerEl.outerHeight()
          left: @options.triggerEl.offset().left
      else if @options.bottomRight
        @$el.addClass 'bubble-popup-bottom-right'
        @$el.css
          top: @options.triggerEl.offset().top + @options.triggerEl.outerHeight()
          right: $(window).width() - @options.triggerEl.offset().left - @options.triggerEl.outerWidth()
      else
        @$el.css
          top: @options.triggerEl.offset().top
          left: @options.triggerEl.offset().left + @options.triggerEl.outerWidth()

      @attachCloseEvents()


    attachCloseEvents: ->
      $('body').on 'click.bubble-popup', =>
        $('body').off 'click.bubble-popup'
        @close()

      @options.triggerEl.on 'click.bubble-popup', (e) =>
        @options.triggerEl.off 'click.bubble-popup'
        e.stopPropagation()
        @close()


    onClose: ->
      $('body').off 'click.bubble-popup'
      @options.triggerEl.off 'click.bubble-popup'

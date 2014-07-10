define [
  'backbone.marionette'
], (
  Marionette
) ->

  $ = jQuery


  class CoveragePopupView extends Marionette.ItemView
    className: 'component-viewer-popup'


    onRender: ->
      @$el.detach().appendTo $('body')

      if @options.bottom
        @$el.addClass 'component-viewer-popup-bottom'
        @$el.css
          top: @options.triggerEl.offset().top + @options.triggerEl.outerHeight()
          left: @options.triggerEl.offset().left
      else if @options.bottomRight
        @$el.addClass 'component-viewer-popup-bottom-right'
        @$el.css
          top: @options.triggerEl.offset().top + @options.triggerEl.outerHeight()
          right: $(window).width() - @options.triggerEl.offset().left - @options.triggerEl.outerWidth()
      else
        @$el.css
          top: @options.triggerEl.offset().top
          left: @options.triggerEl.offset().left + @options.triggerEl.outerWidth()


      $('body').on 'click.coverage-popup', =>
        $('body').off 'click.coverage-popup'
        @close()
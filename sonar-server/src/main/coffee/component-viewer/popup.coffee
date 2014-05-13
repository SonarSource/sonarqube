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

      unless @options.bottom
        @$el.css
          top: @options.triggerEl.offset().top
          left: @options.triggerEl.offset().left + @options.triggerEl.outerWidth()
      else
        @$el.addClass 'component-viewer-popup-bottom'
        @$el.css
          top: @options.triggerEl.offset().top + @options.triggerEl.outerHeight()
          left: @options.triggerEl.offset().left

      $('body').on 'click.coverage-popup', =>
        $('body').off 'click.coverage-popup'
        @close()
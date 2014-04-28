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
      @$el.css
        top: @options.triggerEl.offset().top
        left: @options.triggerEl.offset().left + @options.triggerEl.outerWidth()

      $('body').on 'click.coverage-popup', =>
        $('body').off 'click.coverage-popup'
        @close()
define [
  'backbone.marionette'
  'templates/component-viewer'
], (
  Marionette
  Templates
) ->

  $ = jQuery


  class CoveragePopupView extends Marionette.ItemView
    className: 'component-viewer-popup'
    template: Templates['coveragePopup']


    events:
      'click a[data-key]': 'goToFile'


    onRender: ->
      @$el.detach().appendTo $('body')
      @$el.css 'top', @options.triggerEl.offset().top

      $('body').on 'click.coverage-popup', =>
        $('body').off 'click.coverage-popup'
        @close()


    goToFile: (e) ->
      key = $(e.currentTarget).data 'key'
      console.log key
      @options.main.addTransition key, 'coverage'

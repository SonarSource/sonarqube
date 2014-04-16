define [
  'backbone.marionette'
  'templates/component-viewer'
], (
  Marionette
  Templates
) ->

  $ = jQuery


  class HeaderView extends Marionette.ItemView
    template: Templates['header']


    events:
      'click [data-option=coverage]': 'toggleCoverage'


    onRender: ->
      @delegateEvents()


    toggleCoverage: (e) ->
      el = $(e.currentTarget)
      active = el.is '.active'
      el.toggleClass 'active'
      if active then @options.main.hideCoverage() else @options.main.showCoverage()


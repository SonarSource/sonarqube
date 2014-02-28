define [
  'backbone.marionette',
  'handlebars'
], (
  Marionette,
  Handlebars
) ->

  class QualityGateSidebarListItemView extends Marionette.ItemView
    tagName: 'li'
    template: Handlebars.compile jQuery('#quality-gate-sidebar-list-item-template').html()


    modelEvents:
      'change': 'render'


    events:
      'click': 'showQualityGate'


    onRender: ->
      @$el.toggleClass 'active', @options.highlighted


    showQualityGate: ->
      @options.app.router.navigate "show/#{@model.id}", trigger: true

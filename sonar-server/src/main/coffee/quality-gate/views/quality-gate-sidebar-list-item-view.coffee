define [
  'backbone.marionette',
  'templates/quality-gates'
], (
  Marionette,
  Templates
) ->

  class QualityGateSidebarListItemView extends Marionette.ItemView
    tagName: 'li'
    template: Templates['quality-gate-sidebar-list-item']


    modelEvents:
      'change': 'render'


    events:
      'click': 'showQualityGate'


    onRender: ->
      @$el.toggleClass 'active', @options.highlighted


    showQualityGate: ->
      @options.app.router.navigate "show/#{@model.id}", trigger: true

define [
  'templates/quality-gates'
], ->

  class QualityGateSidebarListItemView extends Marionette.ItemView
    className: 'facet search-navigator-facet'
    template: Templates['quality-gate-sidebar-list-item']


    modelEvents:
      'change': 'render'


    events:
      'click': 'showQualityGate'


    onRender: ->
      @$el.toggleClass 'active', @options.highlighted


    showQualityGate: ->
      @options.app.router.navigate "show/#{@model.id}", trigger: true

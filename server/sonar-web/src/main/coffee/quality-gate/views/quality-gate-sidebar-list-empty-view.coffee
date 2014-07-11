define [
  'backbone.marionette',
  'templates/quality-gates'
], (
  Marionette,
  Templates
) ->

  class QualityGateSidebarListEmptyView extends Marionette.ItemView
    tagName: 'li'
    className: 'empty'
    template: Templates['quality-gate-sidebar-list-empty']

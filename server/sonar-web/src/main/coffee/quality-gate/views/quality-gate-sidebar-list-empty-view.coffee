define [
  'templates/quality-gates'
], ->

  class QualityGateSidebarListEmptyView extends Marionette.ItemView
    tagName: 'li'
    className: 'empty'
    template: Templates['quality-gate-sidebar-list-empty']

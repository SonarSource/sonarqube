define [
  'backbone.marionette',
  'handlebars'
], (
  Marionette,
  Handlebars
) ->

  class QualityGateSidebarListEmptyView extends Marionette.ItemView
    tagName: 'li'
    className: 'empty'
    template: Handlebars.compile jQuery('#quality-gate-sidebar-list-empty-template').html()

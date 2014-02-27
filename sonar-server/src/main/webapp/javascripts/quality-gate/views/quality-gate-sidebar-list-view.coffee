define [
  'backbone.marionette',
  'handlebars',
  'quality-gate/models/quality-gate',
  'quality-gate/views/quality-gate-sidebar-list-item-view'
], (
  Marionette,
  Handlebars,
  QualityGate,
  QualityGateSidebarListItemView
) ->

  class QualityGateSidebarListView extends Marionette.CompositeView
    tagName: 'ul'
    className: 'sidebar blue-sidebar'
    template: Handlebars.compile jQuery('#quality-gate-sidebar-list-template').html()
    itemView: QualityGateSidebarListItemView


    ui:
      spacer: '.spacer'


    events:
      'click #quality-gate-add': 'addQualityGate'


    itemViewOptions: (model) ->
      app: @options.app
      highlighted: model.get('id') == +@highlighted


    appendHtml: (compositeView, itemView) ->
      itemView.$el.insertBefore @ui.spacer


    highlight: (id) ->
      @highlighted = id
      @render()


    addQualityGate: ->
      @options.app.router.navigate 'new', trigger: true

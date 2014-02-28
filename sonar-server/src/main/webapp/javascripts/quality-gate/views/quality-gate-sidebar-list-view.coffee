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

  class QualityGateSidebarListView extends Marionette.CollectionView
    tagName: 'ol'
    className: 'navigator-results-list'
    itemView: QualityGateSidebarListItemView


    itemViewOptions: (model) ->
      app: @options.app
      highlighted: model.get('id') == +@highlighted


    highlight: (id) ->
      @highlighted = id
      @render()

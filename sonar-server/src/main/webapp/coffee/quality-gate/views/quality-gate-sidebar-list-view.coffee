define [
  'backbone.marionette',
  'handlebars',
  'quality-gate/models/quality-gate',
  'quality-gate/views/quality-gate-sidebar-list-item-view',
  'quality-gate/views/quality-gate-sidebar-list-empty-view'
], (
  Marionette,
  Handlebars,
  QualityGate,
  QualityGateSidebarListItemView,
  QualityGateSidebarListEmptyView
) ->

  class QualityGateSidebarListView extends Marionette.CollectionView
    tagName: 'ol'
    className: 'navigator-results-list'
    itemView: QualityGateSidebarListItemView
    emptyView: QualityGateSidebarListEmptyView


    itemViewOptions: (model) ->
      app: @options.app
      highlighted: model.get('id') == +@highlighted


    highlight: (id) ->
      @highlighted = id
      @render()

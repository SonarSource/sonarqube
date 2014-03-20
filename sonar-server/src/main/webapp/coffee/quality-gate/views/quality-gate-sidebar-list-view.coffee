define [
  'backbone.marionette',
  'quality-gate/views/quality-gate-sidebar-list-item-view',
  'quality-gate/views/quality-gate-sidebar-list-empty-view'
], (
  Marionette,
  QualityGateSidebarListItemView,
  QualityGateSidebarListEmptyView,
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

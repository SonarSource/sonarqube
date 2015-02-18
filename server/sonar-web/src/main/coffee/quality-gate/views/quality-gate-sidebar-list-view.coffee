define [
  'quality-gate/views/quality-gate-sidebar-list-item-view',
  'quality-gate/views/quality-gate-sidebar-list-empty-view'
], (
  QualityGateSidebarListItemView,
  QualityGateSidebarListEmptyView,
) ->

  class QualityGateSidebarListView extends Marionette.CollectionView
    className: 'search-navigator-facet-list'
    itemView: QualityGateSidebarListItemView
    emptyView: QualityGateSidebarListEmptyView


    itemViewOptions: (model) ->
      app: @options.app
      highlighted: model.get('id') == +@highlighted


    highlight: (id) ->
      @highlighted = id
      @render()

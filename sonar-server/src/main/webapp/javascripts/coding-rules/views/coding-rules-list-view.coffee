define [
  'backbone.marionette',
  'coding-rules/views/coding-rules-list-item-view',
  'coding-rules/views/coding-rules-list-empty-view'
], (
  Marionette,
  CodingRulesListItemView,
  CodingRulesListEmptyView
) ->

  class CodingRulesListView extends Marionette.CollectionView
    tagName: 'ol'
    className: 'navigator-results-list'
    itemView: CodingRulesListItemView,
    emptyView: CodingRulesListEmptyView,


    itemViewOptions: ->
      listView: @, app: @options.app


    selectFirst: ->
      @$el.find('*:first').click()

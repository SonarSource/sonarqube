define [
  'backbone.marionette',
  'coding-rules/views/coding-rules-facets-item-view'
], (
  Marionette,
  CodingRulesFacetsItemView
) ->

  class CodingRulesFacetsView extends Marionette.CollectionView
    tagName: 'ul'
    className: 'navigator-facets-list'
    itemView: CodingRulesFacetsItemView

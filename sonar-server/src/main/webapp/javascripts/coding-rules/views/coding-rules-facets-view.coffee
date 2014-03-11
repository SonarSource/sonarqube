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


    ui:
      options: '.navigator-facets-list-item-option'


    events:
      'click @ui.options': 'toggleOption'


    toggleOption: (e) ->
      jQuery(e.currentTarget).toggleClass 'active'
      @options.app.fetchFirstPage false

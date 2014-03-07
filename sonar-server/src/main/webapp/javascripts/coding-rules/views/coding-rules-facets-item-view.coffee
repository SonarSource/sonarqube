define [
  'backbone.marionette',
  'common/handlebars-extensions'
], (
  Marionette,
) ->

  class CodingRulesFacetsView extends Marionette.ItemView
    tagName: 'li'
    className: 'navigator-facets-list-item'
    template: getTemplate '#coding-rules-facets-item-template'

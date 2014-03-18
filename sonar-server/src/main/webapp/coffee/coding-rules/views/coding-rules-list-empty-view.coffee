define [
  'backbone.marionette',
  'common/handlebars-extensions'
], (
  Marionette,
) ->

  class CodingRulesListEmptyView extends Marionette.ItemView
    tagName: 'li'
    className: 'navigator-results-no-results'
    template: getTemplate '#coding-rules-list-empty-template'

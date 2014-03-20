define [
  'backbone.marionette',
  'templates/coding-rules'
], (
  Marionette,
  Templates
) ->

  class CodingRulesListEmptyView extends Marionette.ItemView
    tagName: 'li'
    className: 'navigator-results-no-results'
    template: Templates['coding-rules-list-empty']

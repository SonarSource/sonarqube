define [
  'backbone.marionette',
  'common/handlebars-extensions'
], (
  Marionette,
) ->

  class CodingRulesDetailView extends Marionette.ItemView
    template: getTemplate '#coding-rules-detail-template'

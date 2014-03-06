define [
  'backbone.marionette',
  'common/handlebars-extensions'
], (
  Marionette
) ->

  class CodingRulesHeaderView extends Marionette.ItemView
    template: getTemplate '#coding-rules-header-template'

define [
  'backbone.marionette'
  'coding-rules/views/coding-rules-detail-custom-rule-view'
], (
  Marionette
  CodingRulesDetailCustomRuleView
) ->

  class CodingRulesDetailCustomRulesView extends Marionette.CollectionView
    itemView: CodingRulesDetailCustomRuleView

    itemViewOptions: ->
      app: @options.app
      templateRule: @options.templateRule

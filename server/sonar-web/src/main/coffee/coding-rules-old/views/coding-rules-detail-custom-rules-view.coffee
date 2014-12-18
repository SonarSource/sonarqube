define [
  'backbone.marionette'
  'coding-rules-old/views/coding-rules-detail-custom-rule-view'
], (
  Marionette
  CodingRulesDetailCustomRuleView
) ->

  class CodingRulesDetailCustomRulesView extends Marionette.CollectionView
    tagName: 'table'
    className: 'width100'
    itemView: CodingRulesDetailCustomRuleView

    itemViewOptions: ->
      app: @options.app
      templateRule: @options.templateRule

define [
  'backbone.marionette'
  'templates/coding-rules'
], (
  Marionette
  Templates
) ->

  class CodingRulesDetailCustomRuleView extends Marionette.ItemView
    className: 'coding-rules-detail-custom-rule'
    template: Templates['coding-rules-detail-custom-rule']

    ui:
      delete: '.coding-rules-detail-custom-rule-delete'

    events:
      'click @ui.delete': 'delete'

    delete: ->
      confirm('Are you sure ?')


    serializeData: ->
      _.extend super,
        templateRule: @options.templateRule
        canWrite: @options.app.canWrite

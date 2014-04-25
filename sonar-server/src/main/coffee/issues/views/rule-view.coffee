define [
  'backbone.marionette'
  'templates/issues'
], (
  Marionette
  Templates
) ->

  class IssueDetailRuleView extends Marionette.ItemView
    className: 'rule-desc'
    template: Templates['rule']

    modelEvents:
      'change': 'render'


    serializeData: ->
      _.extend super,
        characteristic: this.options.issue.get 'characteristic'
        subCharacteristic: this.options.issue.get 'subCharacteristic'
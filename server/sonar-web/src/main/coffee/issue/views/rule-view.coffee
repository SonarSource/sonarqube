define [
  'backbone.marionette'
  'templates/issue'
], (
  Marionette
  Templates
) ->

  class IssueDetailRuleView extends Marionette.ItemView
    template: Templates['rule']

    modelEvents:
      'change': 'render'


    serializeData: ->
      _.extend super,
        characteristic: this.options.issue.get 'characteristic'
        subCharacteristic: this.options.issue.get 'subCharacteristic'

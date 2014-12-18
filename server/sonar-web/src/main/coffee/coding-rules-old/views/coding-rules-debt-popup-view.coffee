define [
  'backbone.marionette'
  'templates/coding-rules-old'
  'common/popup'
], (
  Marionette
  Templates
  Popup
) ->

  class CodingRulesDebtPopupView extends Popup
    template: Templates['coding-rules-debt-popup']

    serializeData: ->
      _.extend super,
        subcharacteristic: @options.app.getSubcharacteristicName(@model.get 'debtSubChar')

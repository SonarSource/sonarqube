define [
  'backbone.marionette'
  'templates/coding-rules'
  'common/popup'
], (
  Marionette
  Templates
  Popup
) ->

  $ = jQuery


  class CodingRulesParameterPopupView extends Popup
    template: Templates['coding-rules-parameter-popup']

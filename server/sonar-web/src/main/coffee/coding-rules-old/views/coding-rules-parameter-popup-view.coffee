define [
  'backbone.marionette'
  'templates/coding-rules-old'
  'common/popup'
], (
  Marionette
  Templates
  Popup
) ->

  $ = jQuery


  class CodingRulesParameterPopupView extends Popup
    template: Templates['coding-rules-parameter-popup']

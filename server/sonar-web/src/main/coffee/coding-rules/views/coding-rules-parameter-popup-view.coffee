define [
  'backbone.marionette'
  'templates/coding-rules'
  'component-viewer/popup'
#  'component-viewer/utils'
], (
  Marionette
  Templates
  Popup
#  utils
) ->

  $ = jQuery


  class CodingRulesParameterPopupView extends Popup
    template: Templates['coding-rules-parameter-popup']

define [
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/popup'
], (
  Marionette
  Templates
  Popup
) ->

  $ = jQuery


  class TimeChangesPopupView extends Popup
    template: Templates['time-changes-popup']

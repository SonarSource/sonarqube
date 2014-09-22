define [
  'backbone.marionette',
  'templates/quality-gates'
], (
  Marionette,
  Templates
) ->

  class QualityGateDetailConditionsView extends Marionette.ItemView
    tagName: 'tr'
    template: Templates['quality-gate-detail-conditions-empty']

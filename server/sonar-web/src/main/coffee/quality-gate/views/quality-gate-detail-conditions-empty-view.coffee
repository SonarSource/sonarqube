define [
  'templates/quality-gates'
], ->

  class QualityGateDetailConditionsView extends Marionette.ItemView
    tagName: 'tr'
    template: Templates['quality-gate-detail-conditions-empty']

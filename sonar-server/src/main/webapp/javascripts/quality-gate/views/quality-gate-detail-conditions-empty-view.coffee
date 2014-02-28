define [
  'backbone.marionette',
  'handlebars',
], (
  Marionette,
  Handlebars,
) ->

  class QualityGateDetailConditionsView extends Marionette.ItemView
    tagName: 'tr'
    template: Handlebars.compile jQuery('#quality-gate-detail-conditions-empty-template').html()

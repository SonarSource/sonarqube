define [
  'backbone.marionette',
  'templates/quality-gates'
  'quality-gate/models/quality-gate'
], (
  Marionette,
  Templates
  QualityGate
) ->

  class QualityGateActionsView extends Marionette.ItemView
    template: Templates['quality-gate-actions']


    events:
      'click #quality-gate-add': 'add'


    add: ->
      qualityGate = new QualityGate()
      @options.app.qualityGateEditView.method = 'create'
      @options.app.qualityGateEditView.model = qualityGate
      @options.app.qualityGateEditView.show()


    serializeData: ->
      _.extend super, canEdit: @options.app.canEdit

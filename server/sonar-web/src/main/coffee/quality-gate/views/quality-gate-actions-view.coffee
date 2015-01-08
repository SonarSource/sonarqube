define [
  'quality-gate/models/quality-gate'
  'templates/quality-gates'
], (
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

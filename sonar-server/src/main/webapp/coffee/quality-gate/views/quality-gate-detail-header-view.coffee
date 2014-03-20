define [
  'backbone.marionette',
  'templates/quality-gates',
  'quality-gate/models/quality-gate'
], (
  Marionette,
  Templates
  QualityGate
) ->

  class QualityGateDetailHeaderView extends Marionette.ItemView
    template: Templates['quality-gate-detail-header']
    spinner: '<i class="spinner"></i>'


    modelEvents:
      'change': 'render'


    events:
      'click #quality-gate-rename': 'renameQualityGate'
      'click #quality-gate-copy': 'copyQualityGate'
      'click #quality-gate-delete': 'deleteQualityGate'
      'click #quality-gate-set-as-default': 'setAsDefault'
      'click #quality-gate-unset-as-default': 'unsetAsDefault'


    renameQualityGate: ->
      @options.app.qualityGateEditView.method = 'rename'
      @options.app.qualityGateEditView.model = @model
      @options.app.qualityGateEditView.show()


    copyQualityGate: ->
      copiedModel = new QualityGate @model.toJSON()
      copiedModel.set 'default', false
      @options.app.qualityGateEditView.method = 'copy'
      @options.app.qualityGateEditView.model = copiedModel
      @options.app.qualityGateEditView.show()


    deleteQualityGate: ->
      if confirm t('are_you_sure')
        @showSpinner()
        jQuery.ajax
          type: 'POST'
          url: "#{baseUrl}/api/qualitygates/destroy"
          data: id: @model.id
        .always =>
          @hideSpinner()
        .done =>
          @options.app.deleteQualityGate @model.id


    changeDefault: (set) ->
      @showSpinner()
      data = if set then { id: @model.id } else {}
      method = if set then 'set_as_default' else 'unset_default'
      jQuery.ajax
        type: 'POST'
        url: "#{baseUrl}/api/qualitygates/#{method}"
        data: data
      .always =>
        @hideSpinner()
      .done =>
        @options.app.unsetDefaults @model.id
        @model.set 'default', !@model.get('default')


    setAsDefault: ->
      @changeDefault true


    unsetAsDefault: ->
      @changeDefault false


    showSpinner: ->
      @$el.hide()
      jQuery(@spinner).insertBefore @$el


    hideSpinner: ->
      @$el.prev().remove()
      @$el.show()


    serializeData: ->
      _.extend super, canEdit: @options.app.canEdit

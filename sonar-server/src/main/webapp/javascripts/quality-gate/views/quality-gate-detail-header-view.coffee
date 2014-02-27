define [
  'backbone.marionette',
  'handlebars'
], (
  Marionette,
  Handlebars,
) ->

  class QualityGateDetailHeaderView extends Marionette.ItemView
    template: Handlebars.compile jQuery('#quality-gate-detail-header-template').html()


    events:
      'click #quality-gate-rename': 'renameQualityGate'
      'click #quality-gate-delete': 'deleteQualityGate'
      'click #quality-gate-set-as-default': 'setAsDefault'
      'click #quality-gate-unset-as-default': 'unsetAsDefault'


    renameQualityGate: ->
      @options.detailView.showRenaming()


    deleteQualityGate: ->
      if confirm window.SS.phrases.areYouSure
        @options.detailView.showHeaderSpinner()
        jQuery.ajax({
          type: 'POST'
          url: "#{baseUrl}/api/qualitygates/destroy"
          data: id: @model.id
        }).done =>
          @options.app.deleteQualityGate @model.id


    changeDefault: (set) ->
      @options.detailView.showHeaderSpinner()
      data = if set then { id: @model.id } else {}
      method = if set then 'set_as_default' else 'unset_default'
      jQuery.ajax({
        type: 'POST'
        url: "#{baseUrl}/api/qualitygates/#{method}"
        data: data
      }).done =>
        @options.app.unsetDefaults @model.id
        @model.set 'default', !@model.get('default')
        @options.detailView.hideHeaderSpinner()


    setAsDefault: ->
      @changeDefault true


    unsetAsDefault: ->
      @changeDefault false

define [
  'backbone.marionette',
  'templates/quality-gates'
  'select-list'
], (
  Marionette,
  Templates
) ->

  class QualityGateDetailProjectsView extends Marionette.ItemView
    template: Templates['quality-gate-detail-projects']


    onRender: ->
      unless @model.get('default')
        new SelectList
          el: @$('#select-list-projects')
          width: '100%'
          readOnly: !@options.app.canEdit
          format: (item) -> item.name
          searchUrl: "#{baseUrl}/api/qualitygates/search?gateId=#{@options.gateId}"
          selectUrl: "#{baseUrl}/api/qualitygates/select"
          deselectUrl: "#{baseUrl}/api/qualitygates/deselect"
          extra:
            gateId: @options.gateId
          selectParameter: 'projectId'
          selectParameterValue: 'id'
          labels:
            selected: t('quality_gates.projects.with')
            deselected: t('quality_gates.projects.without')
            all: t('quality_gates.projects.all')
            noResults: t('quality_gates.projects.noResults')
          tooltips:
            select: t('quality_gates.projects.select_hint')
            deselect: t('quality_gates.projects.deselect_hint')

    serializeData: ->
      _.extend super, canEdit: @options.app.canEdit

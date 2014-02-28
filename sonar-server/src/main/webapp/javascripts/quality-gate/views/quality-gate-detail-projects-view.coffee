define [
  'backbone.marionette',
  'handlebars',
  'select-list'
], (
  Marionette,
  Handlebars
) ->

  class QualityGateDetailProjectsView extends Marionette.ItemView
    template: Handlebars.compile jQuery('#quality-gate-detail-projects-template').html()


    onRender: ->
      unless @model.get('default')
        @$el.css 'max-width', 600
        new SelectList
          el: @$('#select-list-projects')
          width: '100%'
          format: (item) -> item.name
          searchUrl: "#{baseUrl}/api/qualitygates/search?gateId=#{@options.gateId}"
          selectUrl: "#{baseUrl}/api/qualitygates/select"
          deselectUrl: "#{baseUrl}/api/qualitygates/deselect"
          extra:
            gateId: @options.gateId
          selectParameter: 'projectId'
          selectParameterValue: 'id'
          labels:
            selected: window.SS.phrases.projects.with
            deselected: window.SS.phrases.projects.without
            all: window.SS.phrases.projects.all
          tooltips:
            select: window.SS.phrases.projects.select_hint
            deselect: window.SS.phrases.projects.deselect_hint

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
          selected: 'With'
          deselected: 'Without'
          all: 'All'
        tooltips:
          select: 'Click to add this user to the group <%= h @group.name -%>'
          deselect: 'Click to remove this member from the group <%= h @group.name -%>'

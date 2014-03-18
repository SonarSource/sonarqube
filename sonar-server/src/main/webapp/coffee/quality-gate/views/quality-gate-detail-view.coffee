define [
  'backbone.marionette',
  'handlebars',
  'quality-gate/collections/conditions',
  'quality-gate/views/quality-gate-detail-header-view',
  'quality-gate/views/quality-gate-detail-conditions-view',
  'quality-gate/views/quality-gate-detail-projects-view'
], (
  Marionette,
  Handlebars,
  Conditions,
  QualityGateDetailHeaderView,
  QualityGateDetailConditionsView,
  QualityGateDetailProjectsView
) ->

  class QualityGateDetailView extends Marionette.Layout
    template: Handlebars.compile jQuery('#quality-gate-detail-template').html()


    regions:
      conditionsRegion: '#quality-gate-conditions'
      projectsRegion: '#quality-gate-projects'


    modelEvents:
      'change': 'render'


    onRender: ->
      @showConditions()
      @showProjects()


    showConditions: ->
      conditions = new Conditions @model.get('conditions')
      view = new QualityGateDetailConditionsView
        app: @options.app
        collection: conditions
        gateId: @model.id
        qualityGate: @model
      @conditionsRegion.show view


    showProjects: ->
      view = new QualityGateDetailProjectsView
        app: @options.app
        model: @model
        gateId: @model.id
      @projectsRegion.show view

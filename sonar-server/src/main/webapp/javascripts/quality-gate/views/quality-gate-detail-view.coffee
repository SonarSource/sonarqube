define [
  'backbone.marionette',
  'handlebars',
  '../collections/conditions',
  '../views/quality-gate-detail-header-view',
  '../views/quality-gate-detail-renaming-view',
  '../views/quality-gate-detail-conditions-view',
  '../views/quality-gate-detail-projects-view'
], (
  Marionette,
  Handlebars,
  Conditions,
  QualityGateDetailHeaderView,
  QualityGateDetailRenamingView,
  QualityGateDetailConditionsView,
  QualityGateDetailProjectsView
) ->

  class QualityGateDetailView extends Marionette.Layout
    className: 'quality-gate'
    template: Handlebars.compile jQuery('#quality-gate-detail-template').html()


    regions:
      headerRegion: '.quality-gate-header'
      tabRegion: '.quality-gate-details-tab'


    ui:
      tabs: '.quality-gate-tabs'
      conditionsTab: '#quality-gate-tab-conditions'
      projectsTab: '#quality-gate-tab-projects'


    modelEvents:
      'change': 'render'


    events:
      'click @ui.conditionsTab': 'showConditions'
      'click @ui.projectsTab': 'showProjects'


    onRender: ->
      @showHeader()
      @showConditions()


    showHeader: ->
      view = new QualityGateDetailHeaderView
        app: @options.app
        detailView: @
        model: @model
      @headerRegion.show view


    showRenaming: ->
      view = new QualityGateDetailRenamingView
        app: @options.app
        detailView: @
        model: @model
      @headerRegion.show view


    showConditions: ->
      @ui.tabs.find('a').removeClass 'selected'
      @ui.conditionsTab.addClass 'selected'
      conditions = new Conditions @model.get('conditions')
      view = new QualityGateDetailConditionsView
        app: @options.app
        collection: conditions
        gateId: @model.id
        qualityGate: @model
      @tabRegion.show view


    showProjects: ->
      @ui.tabs.find('a').removeClass 'selected'
      @ui.projectsTab.addClass 'selected'
      view = new QualityGateDetailProjectsView
        app: @options.app
        gateId: @model.id
      @tabRegion.show view


    showHeaderSpinner: ->
      @$(@headerRegion.el).addClass 'navigator-fetching'


    hideHeaderSpinner: ->
      @$(@headerRegion.el).removeClass 'navigator-fetching'

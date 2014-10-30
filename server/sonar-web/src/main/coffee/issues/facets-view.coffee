define [
  'backbone.marionette'

  'issues/facets/base-facet'
  'issues/facets/severity-facet'
  'issues/facets/status-facet'
  'issues/facets/project-facet'
  'issues/facets/assignee-facet'
  'issues/facets/rule-facet'
  'issues/facets/resolution-facet'
  'issues/facets/creation-date-facet'
  'issues/facets/action-plan-facet'
  'issues/facets/component-facet'
  'issues/facets/reporter-facet'
], (
  Marionette
  BaseFacet
  SeverityFacet
  StatusFacet
  ProjectFacet
  AssigneeFacet
  RuleFacet
  ResolutionFacet
  CreationDateFacet
  ActionPlanFacet
  ComponentFacet
  ReporterFacet
) ->

  class extends Marionette.CollectionView
    className: 'issues-facets-list'


    getItemView: (model) ->
      switch model.get 'property'
        when 'severities' then SeverityFacet
        when 'statuses' then StatusFacet
        when 'assignees' then AssigneeFacet
        when 'resolutions' then ResolutionFacet
        when 'created' then CreationDateFacet
        when 'componentRootUuids' then ProjectFacet
        when 'rules' then RuleFacet
        when 'actionPlans' then ActionPlanFacet
        when 'componentUuids' then ComponentFacet
        when 'reporters' then ReporterFacet
        else BaseFacet


    itemViewOptions: ->
      app: @options.app


    collectionEvents: ->
      'change:enabled': 'updateState'


    updateState: ->
      enabledFacets = @collection.filter (model) -> model.get('enabled')
      enabledFacetIds = enabledFacets.map (model) -> model.id
      @options.app.state.set facets: enabledFacetIds


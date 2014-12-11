define [
  'backbone.marionette'

  'issues/facets/base-facet'
  'issues/facets/severity-facet'
  'issues/facets/status-facet'
  'issues/facets/project-facet'
  'issues/facets/module-facet'
  'issues/facets/assignee-facet'
  'issues/facets/rule-facet'
  'issues/facets/tag-facet'
  'issues/facets/resolution-facet'
  'issues/facets/creation-date-facet'
  'issues/facets/action-plan-facet'
  'issues/facets/component-facet'
  'issues/facets/reporter-facet'
  'issues/facets/language-facet'
], (
  Marionette
  BaseFacet
  SeverityFacet
  StatusFacet
  ProjectFacet
  ModuleFacet
  AssigneeFacet
  RuleFacet
  TagFacet
  ResolutionFacet
  CreationDateFacet
  ActionPlanFacet
  ComponentFacet
  ReporterFacet
  LanguageFacet
) ->

  class extends Marionette.CollectionView
    className: 'issues-facets-list'


    getItemView: (model) ->
      switch model.get 'property'
        when 'severities' then SeverityFacet
        when 'statuses' then StatusFacet
        when 'assignees' then AssigneeFacet
        when 'resolutions' then ResolutionFacet
        when 'creationDate' then CreationDateFacet
        when 'projectUuids' then ProjectFacet
        when 'componentRootUuids' then ModuleFacet
        when 'rules' then RuleFacet
        when 'tags' then TagFacet
        when 'actionPlans' then ActionPlanFacet
        when 'componentUuids' then ComponentFacet
        when 'reporters' then ReporterFacet
        when 'languages' then LanguageFacet
        else BaseFacet


    itemViewOptions: ->
      app: @options.app


    collectionEvents: ->
      'change:enabled': 'updateState'


    updateState: ->
      enabledFacets = @collection.filter (model) -> model.get('enabled')
      enabledFacetIds = enabledFacets.map (model) -> model.id
      @options.app.state.set facets: enabledFacetIds

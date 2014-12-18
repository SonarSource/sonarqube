define [
  'components/navigator/facets-view'

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
  'issues/facets/issue-key-facet'
], (
  FacetsView

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
  IssueKeyFacet
) ->

  class extends FacetsView

    getItemView: (model) ->
      switch model.get 'property'
        when 'severities' then SeverityFacet
        when 'statuses' then StatusFacet
        when 'assignees' then AssigneeFacet
        when 'resolutions' then ResolutionFacet
        when 'creationDate' then CreationDateFacet
        when 'projectUuids' then ProjectFacet
        when 'moduleUuids' then ModuleFacet
        when 'rules' then RuleFacet
        when 'tags' then TagFacet
        when 'actionPlans' then ActionPlanFacet
        when 'componentUuids' then ComponentFacet
        when 'reporters' then ReporterFacet
        when 'languages' then LanguageFacet
        when 'issues' then IssueKeyFacet
        else BaseFacet

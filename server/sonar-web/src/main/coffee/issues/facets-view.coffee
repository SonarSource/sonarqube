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
  'issues/facets/file-facet'
  'issues/facets/reporter-facet'
  'issues/facets/language-facet'
  'issues/facets/author-facet'
  'issues/facets/issue-key-facet'
  'issues/facets/context-facet'
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
  FileFacet
  ReporterFacet
  LanguageFacet
  AuthorFacet
  IssueKeyFacet
  ContextFacet
) ->

  class extends FacetsView

    getItemView: (model) ->
      switch model.get 'property'
        when 'severities' then SeverityFacet
        when 'statuses' then StatusFacet
        when 'assignees' then AssigneeFacet
        when 'resolutions' then ResolutionFacet
        when 'createdAt' then CreationDateFacet
        when 'projectUuids' then ProjectFacet
        when 'moduleUuids' then ModuleFacet
        when 'rules' then RuleFacet
        when 'tags' then TagFacet
        when 'actionPlans' then ActionPlanFacet
        when 'fileUuids' then FileFacet
        when 'reporters' then ReporterFacet
        when 'languages' then LanguageFacet
        when 'authors' then AuthorFacet
        when 'issues' then IssueKeyFacet
        when 'context' then ContextFacet
        else BaseFacet

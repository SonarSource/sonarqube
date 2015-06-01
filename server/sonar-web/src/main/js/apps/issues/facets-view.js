define([
  'components/navigator/facets-view',
  './facets/base-facet',
  './facets/severity-facet',
  './facets/status-facet',
  './facets/project-facet',
  './facets/module-facet',
  './facets/assignee-facet',
  './facets/rule-facet',
  './facets/tag-facet',
  './facets/resolution-facet',
  './facets/creation-date-facet',
  './facets/action-plan-facet',
  './facets/file-facet',
  './facets/reporter-facet',
  './facets/language-facet',
  './facets/author-facet',
  './facets/issue-key-facet',
  './facets/context-facet'
], function (FacetsView, BaseFacet, SeverityFacet, StatusFacet, ProjectFacet, ModuleFacet, AssigneeFacet, RuleFacet,
             TagFacet, ResolutionFacet, CreationDateFacet, ActionPlanFacet, FileFacet, ReporterFacet, LanguageFacet,
             AuthorFacet, IssueKeyFacet, ContextFacet) {

  return FacetsView.extend({
    getItemView: function (model) {
      switch (model.get('property')) {
        case 'severities':
          return SeverityFacet;
        case 'statuses':
          return StatusFacet;
        case 'assignees':
          return AssigneeFacet;
        case 'resolutions':
          return ResolutionFacet;
        case 'createdAt':
          return CreationDateFacet;
        case 'projectUuids':
          return ProjectFacet;
        case 'moduleUuids':
          return ModuleFacet;
        case 'rules':
          return RuleFacet;
        case 'tags':
          return TagFacet;
        case 'actionPlans':
          return ActionPlanFacet;
        case 'fileUuids':
          return FileFacet;
        case 'reporters':
          return ReporterFacet;
        case 'languages':
          return LanguageFacet;
        case 'authors':
          return AuthorFacet;
        case 'issues':
          return IssueKeyFacet;
        case 'context':
          return ContextFacet;
        default:
          return BaseFacet;
      }
    }
  });

});

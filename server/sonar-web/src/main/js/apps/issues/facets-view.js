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

  var viewsMapping = {
    severities: SeverityFacet,
    statuses: StatusFacet,
    assignees: AssigneeFacet,
    resolutions: ResolutionFacet,
    createdAt: CreationDateFacet,
    projectUuids: ProjectFacet,
    moduleUuids: ModuleFacet,
    rules: RuleFacet,
    tags: TagFacet,
    actionPlans: ActionPlanFacet,
    fileUuids: FileFacet,
    reporters: ReporterFacet,
    languages: LanguageFacet,
    authors: AuthorFacet,
    issues: IssueKeyFacet,
    context: ContextFacet
  };

  return FacetsView.extend({
    getItemView: function (model) {
      var view = viewsMapping[model.get('property')];
      return view ? view : BaseFacet;
    }
  });

});

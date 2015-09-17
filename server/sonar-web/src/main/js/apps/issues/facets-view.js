import FacetsView from 'components/navigator/facets-view';
import BaseFacet from './facets/base-facet';
import SeverityFacet from './facets/severity-facet';
import StatusFacet from './facets/status-facet';
import ProjectFacet from './facets/project-facet';
import ModuleFacet from './facets/module-facet';
import AssigneeFacet from './facets/assignee-facet';
import RuleFacet from './facets/rule-facet';
import TagFacet from './facets/tag-facet';
import ResolutionFacet from './facets/resolution-facet';
import CreationDateFacet from './facets/creation-date-facet';
import ActionPlanFacet from './facets/action-plan-facet';
import FileFacet from './facets/file-facet';
import ReporterFacet from './facets/reporter-facet';
import LanguageFacet from './facets/language-facet';
import AuthorFacet from './facets/author-facet';
import IssueKeyFacet from './facets/issue-key-facet';
import ContextFacet from './facets/context-facet';
import ModeFacet from './facets/mode-facet';

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
  context: ContextFacet,
  facetMode: ModeFacet
};

export default FacetsView.extend({
  getChildView: function (model) {
    var view = viewsMapping[model.get('property')];
    return view ? view : BaseFacet;
  }
});



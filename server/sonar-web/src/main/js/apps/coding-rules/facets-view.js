import FacetsView from 'components/navigator/facets-view';
import BaseFacet from './facets/base-facet';
import QueryFacet from './facets/query-facet';
import KeyFacet from './facets/key-facet';
import LanguageFacet from './facets/language-facet';
import RepositoryFacet from './facets/repository-facet';
import TagFacet from './facets/tag-facet';
import QualityProfileFacet from './facets/quality-profile-facet';
import CharacteristicFacet from './facets/characteristic-facet';
import SeverityFacet from './facets/severity-facet';
import StatusFacet from './facets/status-facet';
import AvailableSinceFacet from './facets/available-since-facet';
import InheritanceFacet from './facets/inheritance-facet';
import ActiveSeverityFacet from './facets/active-severity-facet';
import TemplateFacet from './facets/template-facet';

var viewsMapping = {
  q: QueryFacet,
  rule_key: KeyFacet,
  languages: LanguageFacet,
  repositories: RepositoryFacet,
  tags: TagFacet,
  qprofile: QualityProfileFacet,
  debt_characteristics: CharacteristicFacet,
  severities: SeverityFacet,
  statuses: StatusFacet,
  available_since: AvailableSinceFacet,
  inheritance: InheritanceFacet,
  active_severities: ActiveSeverityFacet,
  is_template: TemplateFacet
};

export default FacetsView.extend({

  getChildView: function (model) {
    var view = viewsMapping[model.get('property')];
    return view ? view : BaseFacet;
  }

});

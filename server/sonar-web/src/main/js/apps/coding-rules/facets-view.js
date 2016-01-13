/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import FacetsView from '../../components/navigator/facets-view';
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

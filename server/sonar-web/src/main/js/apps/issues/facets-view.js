/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import TypeFacet from './facets/type-facet';
import SeverityFacet from './facets/severity-facet';
import StatusFacet from './facets/status-facet';
import ProjectFacet from './facets/project-facet';
import ModuleFacet from './facets/module-facet';
import AssigneeFacet from './facets/assignee-facet';
import RuleFacet from './facets/rule-facet';
import TagFacet from './facets/tag-facet';
import ResolutionFacet from './facets/resolution-facet';
import CreationDateFacet from './facets/creation-date-facet';
import FileFacet from './facets/file-facet';
import LanguageFacet from './facets/language-facet';
import AuthorFacet from './facets/author-facet';
import IssueKeyFacet from './facets/issue-key-facet';
import ContextFacet from './facets/context-facet';
import ModeFacet from './facets/mode-facet';

const viewsMapping = {
  types: TypeFacet,
  severities: SeverityFacet,
  statuses: StatusFacet,
  assignees: AssigneeFacet,
  resolutions: ResolutionFacet,
  createdAt: CreationDateFacet,
  projectUuids: ProjectFacet,
  moduleUuids: ModuleFacet,
  rules: RuleFacet,
  tags: TagFacet,
  fileUuids: FileFacet,
  languages: LanguageFacet,
  authors: AuthorFacet,
  issues: IssueKeyFacet,
  context: ContextFacet,
  facetMode: ModeFacet
};

export default FacetsView.extend({
  getChildView (model) {
    const view = viewsMapping[model.get('property')];
    return view ? view : BaseFacet;
  }
});


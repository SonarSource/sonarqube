/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
import React from 'react';
import AssigneeFacet from './AssigneeFacet';
import AuthorFacet from './AuthorFacet';
import CreationDateFacet from './CreationDateFacet';
import DirectoryFacet from './DirectoryFacet';
import FacetMode from './FacetMode';
import FileFacet from './FileFacet';
import LanguageFacet from './LanguageFacet';
import ModuleFacet from './ModuleFacet';
import ProjectFacet from './ProjectFacet';
import ResolutionFacet from './ResolutionFacet';
import RuleFacet from './RuleFacet';
import SeverityFacet from './SeverityFacet';
import StatusFacet from './StatusFacet';
import TagFacet from './TagFacet';
import TypeFacet from './TypeFacet';
/*:: import type {
  Query,
  Facet,
  ReferencedComponent,
  ReferencedUser,
  ReferencedLanguage,
  Component
} from '../utils'; */

/*::
type Props = {|
  component?: Component,
  facets: { [string]: Facet },
  myIssues: boolean,
  onFacetToggle: (property: string) => void,
  onFilterChange: (changes: { [string]: Array<string> }) => void,
  openFacets: { [string]: boolean },
  organization?: { key: string },
  query: Query,
  referencedComponents: { [string]: ReferencedComponent },
  referencedLanguages: { [string]: ReferencedLanguage },
  referencedRules: { [string]: { name: string } },
  referencedUsers: { [string]: ReferencedUser }
|};
*/

export default class Sidebar extends React.PureComponent {
  /*:: props: Props; */

  render() {
    const { component, facets, openFacets, query } = this.props;

    const displayProjectsFacet /*: boolean */ =
      component == null || !['TRK', 'BRC', 'DIR', 'DEV_PRJ'].includes(component.qualifier);
    const displayModulesFacet = component != null && component.qualifier !== 'DIR';
    const displayDirectoriesFacet = component != null && component.qualifier !== 'DIR';
    const displayFilesFacet = component != null;
    const displayAuthorFacet = component == null || component.qualifier !== 'DEV';

    return (
      <div className="search-navigator-facets-list">
        <FacetMode facetMode={query.facetMode} onChange={this.props.onFilterChange} />
        <TypeFacet
          facetMode={query.facetMode}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.types}
          stats={facets.types}
          types={query.types}
        />
        <SeverityFacet
          facetMode={query.facetMode}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.severities}
          severities={query.severities}
          stats={facets.severities}
        />
        <ResolutionFacet
          facetMode={query.facetMode}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.resolutions}
          resolved={query.resolved}
          resolutions={query.resolutions}
          stats={facets.resolutions}
        />
        <StatusFacet
          facetMode={query.facetMode}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.statuses}
          stats={facets.statuses}
          statuses={query.statuses}
        />
        <CreationDateFacet
          component={component}
          createdAfter={query.createdAfter}
          createdAt={query.createdAt}
          createdBefore={query.createdBefore}
          createdInLast={query.createdInLast}
          facetMode={query.facetMode}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.createdAt}
          sinceLeakPeriod={query.sinceLeakPeriod}
          stats={facets.createdAt}
        />
        <RuleFacet
          facetMode={query.facetMode}
          languages={query.languages}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          organization={this.props.organization && this.props.organization.key}
          open={!!openFacets.rules}
          stats={facets.rules}
          referencedRules={this.props.referencedRules}
          rules={query.rules}
        />
        <TagFacet
          component={component}
          facetMode={query.facetMode}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.tags}
          organization={this.props.organization}
          stats={facets.tags}
          tags={query.tags}
        />
        {displayProjectsFacet && (
          <ProjectFacet
            component={component}
            facetMode={query.facetMode}
            onChange={this.props.onFilterChange}
            onToggle={this.props.onFacetToggle}
            open={!!openFacets.projects}
            organization={this.props.organization}
            projects={query.projects}
            referencedComponents={this.props.referencedComponents}
            stats={facets.projects}
          />
        )}
        {displayModulesFacet && (
          <ModuleFacet
            facetMode={query.facetMode}
            onChange={this.props.onFilterChange}
            onToggle={this.props.onFacetToggle}
            open={!!openFacets.modules}
            modules={query.modules}
            referencedComponents={this.props.referencedComponents}
            stats={facets.modules}
          />
        )}
        {displayDirectoriesFacet && (
          <DirectoryFacet
            facetMode={query.facetMode}
            onChange={this.props.onFilterChange}
            onToggle={this.props.onFacetToggle}
            open={!!openFacets.directories}
            directories={query.directories}
            referencedComponents={this.props.referencedComponents}
            stats={facets.directories}
          />
        )}
        {displayFilesFacet && (
          <FileFacet
            facetMode={query.facetMode}
            onChange={this.props.onFilterChange}
            onToggle={this.props.onFacetToggle}
            open={!!openFacets.files}
            files={query.files}
            referencedComponents={this.props.referencedComponents}
            stats={facets.files}
          />
        )}
        {!this.props.myIssues && (
          <AssigneeFacet
            component={component}
            facetMode={query.facetMode}
            onChange={this.props.onFilterChange}
            onToggle={this.props.onFacetToggle}
            open={!!openFacets.assignees}
            organization={this.props.organization}
            assigned={query.assigned}
            assignees={query.assignees}
            referencedUsers={this.props.referencedUsers}
            stats={facets.assignees}
          />
        )}
        {displayAuthorFacet && (
          <AuthorFacet
            facetMode={query.facetMode}
            onChange={this.props.onFilterChange}
            onToggle={this.props.onFacetToggle}
            open={!!openFacets.authors}
            authors={query.authors}
            stats={facets.authors}
          />
        )}
        <LanguageFacet
          facetMode={query.facetMode}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.languages}
          languages={query.languages}
          referencedLanguages={this.props.referencedLanguages}
          stats={facets.languages}
        />
      </div>
    );
  }
}

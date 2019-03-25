/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import AssigneeFacet from './AssigneeFacet';
import AuthorFacet from './AuthorFacet';
import CreationDateFacet from './CreationDateFacet';
import DirectoryFacet from './DirectoryFacet';
import FileFacet from './FileFacet';
import LanguageFacet from './LanguageFacet';
import ProjectFacet from './ProjectFacet';
import ResolutionFacet from './ResolutionFacet';
import RuleFacet from './RuleFacet';
import SeverityFacet from './SeverityFacet';
import StandardFacet from './StandardFacet';
import StatusFacet from './StatusFacet';
import TagFacet from './TagFacet';
import TypeFacet from './TypeFacet';
import {
  Query,
  Facet,
  ReferencedComponent,
  ReferencedUser,
  ReferencedLanguage,
  ReferencedRule,
  STANDARDS
} from '../utils';

export interface Props {
  component: T.Component | undefined;
  facets: T.Dict<Facet>;
  hideAuthorFacet?: boolean;
  loadSearchResultCount: (property: string, changes: Partial<Query>) => Promise<Facet>;
  loadingFacets: T.Dict<boolean>;
  myIssues: boolean;
  onFacetToggle: (property: string) => void;
  onFilterChange: (changes: Partial<Query>) => void;
  openFacets: T.Dict<boolean>;
  organization: { key: string } | undefined;
  query: Query;
  referencedComponentsById: T.Dict<ReferencedComponent>;
  referencedComponentsByKey: T.Dict<ReferencedComponent>;
  referencedLanguages: T.Dict<ReferencedLanguage>;
  referencedRules: T.Dict<ReferencedRule>;
  referencedUsers: T.Dict<ReferencedUser>;
}

export default class Sidebar extends React.PureComponent<Props> {
  renderComponentFacets() {
    const { component, facets, loadingFacets, openFacets, query } = this.props;
    if (!component) {
      return null;
    }
    const commonProps = {
      componentKey: component.key,
      loadSearchResultCount: this.props.loadSearchResultCount,
      onChange: this.props.onFilterChange,
      onToggle: this.props.onFacetToggle,
      query
    };
    return (
      <>
        {component.qualifier !== 'DIR' && (
          <DirectoryFacet
            directories={query.directories}
            fetching={loadingFacets.directories === true}
            open={!!openFacets.directories}
            stats={facets.directories}
            {...commonProps}
          />
        )}
        <FileFacet
          fetching={loadingFacets.files === true}
          files={query.files}
          open={!!openFacets.files}
          referencedComponents={this.props.referencedComponentsById}
          stats={facets.files}
          {...commonProps}
        />
      </>
    );
  }

  render() {
    const { component, facets, hideAuthorFacet, openFacets, query } = this.props;

    const displayProjectsFacet =
      !component || !['TRK', 'BRC', 'DIR', 'DEV_PRJ'].includes(component.qualifier);
    const displayAuthorFacet = !hideAuthorFacet && (!component || component.qualifier !== 'DEV');

    const organizationKey =
      (component && component.organization) ||
      (this.props.organization && this.props.organization.key);

    return (
      <>
        <TypeFacet
          fetching={this.props.loadingFacets.types === true}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.types}
          stats={facets.types}
          types={query.types}
        />
        <SeverityFacet
          fetching={this.props.loadingFacets.severities === true}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.severities}
          severities={query.severities}
          stats={facets.severities}
        />
        <ResolutionFacet
          fetching={this.props.loadingFacets.resolutions === true}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.resolutions}
          resolutions={query.resolutions}
          resolved={query.resolved}
          stats={facets.resolutions}
        />
        <StatusFacet
          fetching={this.props.loadingFacets.statuses === true}
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
          fetching={this.props.loadingFacets.createdAt === true}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.createdAt}
          sinceLeakPeriod={query.sinceLeakPeriod}
          stats={facets.createdAt}
        />
        <LanguageFacet
          fetching={this.props.loadingFacets.languages === true}
          languages={query.languages}
          loadSearchResultCount={this.props.loadSearchResultCount}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.languages}
          query={query}
          referencedLanguages={this.props.referencedLanguages}
          stats={facets.languages}
        />
        <RuleFacet
          fetching={this.props.loadingFacets.rules === true}
          languages={query.languages}
          loadSearchResultCount={this.props.loadSearchResultCount}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.rules}
          organization={organizationKey}
          query={query}
          referencedRules={this.props.referencedRules}
          rules={query.rules}
          stats={facets.rules}
        />
        <StandardFacet
          cwe={query.cwe}
          cweOpen={!!openFacets.cwe}
          cweStats={facets.cwe}
          fetchingCwe={this.props.loadingFacets.cwe === true}
          fetchingOwaspTop10={this.props.loadingFacets.owaspTop10 === true}
          fetchingSansTop25={this.props.loadingFacets.sansTop25 === true}
          loadSearchResultCount={this.props.loadSearchResultCount}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets[STANDARDS]}
          owaspTop10={query.owaspTop10}
          owaspTop10Open={!!openFacets.owaspTop10}
          owaspTop10Stats={facets.owaspTop10}
          query={query}
          sansTop25={query.sansTop25}
          sansTop25Open={!!openFacets.sansTop25}
          sansTop25Stats={facets.sansTop25}
        />
        <TagFacet
          component={component}
          fetching={this.props.loadingFacets.tags === true}
          loadSearchResultCount={this.props.loadSearchResultCount}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.tags}
          organization={organizationKey}
          query={query}
          stats={facets.tags}
          tags={query.tags}
        />
        {displayProjectsFacet && (
          <ProjectFacet
            component={component}
            fetching={this.props.loadingFacets.projects === true}
            loadSearchResultCount={this.props.loadSearchResultCount}
            onChange={this.props.onFilterChange}
            onToggle={this.props.onFacetToggle}
            open={!!openFacets.projects}
            organization={this.props.organization}
            projects={query.projects}
            query={query}
            referencedComponents={this.props.referencedComponentsByKey}
            stats={facets.projects}
          />
        )}
        {this.renderComponentFacets()}
        {!this.props.myIssues && (
          <AssigneeFacet
            assigned={query.assigned}
            assignees={query.assignees}
            fetching={this.props.loadingFacets.assignees === true}
            loadSearchResultCount={this.props.loadSearchResultCount}
            onChange={this.props.onFilterChange}
            onToggle={this.props.onFacetToggle}
            open={!!openFacets.assignees}
            organization={organizationKey}
            query={query}
            referencedUsers={this.props.referencedUsers}
            stats={facets.assignees}
          />
        )}
        {displayAuthorFacet && (
          <AuthorFacet
            authors={query.authors}
            component={component}
            fetching={this.props.loadingFacets.authors === true}
            loadSearchResultCount={this.props.loadSearchResultCount}
            onChange={this.props.onFilterChange}
            onToggle={this.props.onFacetToggle}
            open={!!openFacets.authors}
            organization={organizationKey}
            query={query}
            stats={facets.authors}
          />
        )}
      </>
    );
  }
}

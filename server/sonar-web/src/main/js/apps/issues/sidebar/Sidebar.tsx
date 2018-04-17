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
import * as React from 'react';
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
import { Query, Facet, ReferencedComponent, ReferencedUser, ReferencedLanguage } from '../utils';
import { Component } from '../../../app/types';

export interface Props {
  component: Component | undefined;
  facets: { [facet: string]: Facet };
  loading?: boolean;
  myIssues: boolean;
  onFacetToggle: (property: string) => void;
  onFilterChange: (changes: Partial<Query>) => void;
  openFacets: { [facet: string]: boolean };
  organization: { key: string } | undefined;
  query: Query;
  referencedComponents: { [componentKey: string]: ReferencedComponent };
  referencedLanguages: { [languageKey: string]: ReferencedLanguage };
  referencedRules: { [ruleKey: string]: { name: string } };
  referencedUsers: { [login: string]: ReferencedUser };
}

export default class Sidebar extends React.PureComponent<Props> {
  render() {
    const { component, facets, openFacets, query } = this.props;

    const displayProjectsFacet =
      !component || !['TRK', 'BRC', 'DIR', 'DEV_PRJ'].includes(component.qualifier);
    const displayModulesFacet = component !== undefined && component.qualifier !== 'DIR';
    const displayDirectoriesFacet = component !== undefined && component.qualifier !== 'DIR';
    const displayFilesFacet = component !== undefined;
    const displayAuthorFacet = !component || component.qualifier !== 'DEV';

    return (
      <div className="search-navigator-facets-list">
        <FacetMode facetMode={query.facetMode} onChange={this.props.onFilterChange} />
        <TypeFacet
          facetMode={query.facetMode}
          loading={this.props.loading}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.types}
          stats={facets.types}
          types={query.types}
        />
        <SeverityFacet
          facetMode={query.facetMode}
          loading={this.props.loading}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.severities}
          severities={query.severities}
          stats={facets.severities}
        />
        <ResolutionFacet
          facetMode={query.facetMode}
          loading={this.props.loading}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.resolutions}
          resolutions={query.resolutions}
          resolved={query.resolved}
          stats={facets.resolutions}
        />
        <StatusFacet
          facetMode={query.facetMode}
          loading={this.props.loading}
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
          loading={this.props.loading}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.createdAt}
          sinceLeakPeriod={query.sinceLeakPeriod}
          stats={facets.createdAt}
        />
        <RuleFacet
          facetMode={query.facetMode}
          languages={query.languages}
          loading={this.props.loading}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.rules}
          organization={this.props.organization && this.props.organization.key}
          referencedRules={this.props.referencedRules}
          rules={query.rules}
          stats={facets.rules}
        />
        <TagFacet
          component={component}
          facetMode={query.facetMode}
          loading={this.props.loading}
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
            loading={this.props.loading}
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
            loading={this.props.loading}
            modules={query.modules}
            onChange={this.props.onFilterChange}
            onToggle={this.props.onFacetToggle}
            open={!!openFacets.modules}
            referencedComponents={this.props.referencedComponents}
            stats={facets.modules}
          />
        )}
        {displayDirectoriesFacet && (
          <DirectoryFacet
            directories={query.directories}
            facetMode={query.facetMode}
            loading={this.props.loading}
            onChange={this.props.onFilterChange}
            onToggle={this.props.onFacetToggle}
            open={!!openFacets.directories}
            referencedComponents={this.props.referencedComponents}
            stats={facets.directories}
          />
        )}
        {displayFilesFacet && (
          <FileFacet
            facetMode={query.facetMode}
            files={query.files}
            loading={this.props.loading}
            onChange={this.props.onFilterChange}
            onToggle={this.props.onFacetToggle}
            open={!!openFacets.files}
            referencedComponents={this.props.referencedComponents}
            stats={facets.files}
          />
        )}
        {!this.props.myIssues && (
          <AssigneeFacet
            assigned={query.assigned}
            assignees={query.assignees}
            component={component}
            facetMode={query.facetMode}
            loading={this.props.loading}
            onChange={this.props.onFilterChange}
            onToggle={this.props.onFacetToggle}
            open={!!openFacets.assignees}
            organization={this.props.organization}
            referencedUsers={this.props.referencedUsers}
            stats={facets.assignees}
          />
        )}
        {displayAuthorFacet && (
          <AuthorFacet
            authors={query.authors}
            facetMode={query.facetMode}
            loading={this.props.loading}
            onChange={this.props.onFilterChange}
            onToggle={this.props.onFacetToggle}
            open={!!openFacets.authors}
            stats={facets.authors}
          />
        )}
        <LanguageFacet
          facetMode={query.facetMode}
          languages={query.languages}
          loading={this.props.loading}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.languages}
          referencedLanguages={this.props.referencedLanguages}
          stats={facets.languages}
        />
      </div>
    );
  }
}

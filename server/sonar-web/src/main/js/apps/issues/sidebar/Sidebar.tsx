/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import { isBranch, isPullRequest } from '../../../helpers/branch-like';
import { AppState } from '../../../types/appstate';
import { BranchLike } from '../../../types/branch-like';
import {
  ComponentQualifier,
  isApplication,
  isPortfolioLike,
  isView,
} from '../../../types/component';
import {
  Facet,
  ReferencedComponent,
  ReferencedLanguage,
  ReferencedRule,
} from '../../../types/issues';
import { GlobalSettingKeys } from '../../../types/settings';
import { Component, Dict } from '../../../types/types';
import { UserBase } from '../../../types/users';
import { Query } from '../utils';
import AssigneeFacet from './AssigneeFacet';
import AuthorFacet from './AuthorFacet';
import CreationDateFacet from './CreationDateFacet';
import DirectoryFacet from './DirectoryFacet';
import FileFacet from './FileFacet';
import LanguageFacet from './LanguageFacet';
import PeriodFilter from './PeriodFilter';
import ProjectFacet from './ProjectFacet';
import ResolutionFacet from './ResolutionFacet';
import RuleFacet from './RuleFacet';
import ScopeFacet from './ScopeFacet';
import SeverityFacet from './SeverityFacet';
import StandardFacet from './StandardFacet';
import StatusFacet from './StatusFacet';
import TagFacet from './TagFacet';
import TypeFacet from './TypeFacet';

export interface Props {
  appState: AppState;
  branchLike?: BranchLike;
  component: Component | undefined;
  createdAfterIncludesTime: boolean;
  facets: Dict<Facet | undefined>;
  loadSearchResultCount: (property: string, changes: Partial<Query>) => Promise<Facet>;
  loadingFacets: Dict<boolean>;
  myIssues: boolean;
  onFacetToggle: (property: string) => void;
  onFilterChange: (changes: Partial<Query>) => void;
  openFacets: Dict<boolean>;
  query: Query;
  referencedComponentsById: Dict<ReferencedComponent>;
  referencedComponentsByKey: Dict<ReferencedComponent>;
  referencedLanguages: Dict<ReferencedLanguage>;
  referencedRules: Dict<ReferencedRule>;
  referencedUsers: Dict<UserBase>;
}

export class Sidebar extends React.PureComponent<Props> {
  renderComponentFacets() {
    const { component, facets, loadingFacets, openFacets, query, branchLike } = this.props;
    const hasFileOrDirectory =
      !isApplication(component?.qualifier) && !isPortfolioLike(component?.qualifier);
    if (!component || !hasFileOrDirectory) {
      return null;
    }
    const commonProps = {
      componentKey: component.key,
      loadSearchResultCount: this.props.loadSearchResultCount,
      onChange: this.props.onFilterChange,
      onToggle: this.props.onFacetToggle,
      query,
    };
    return (
      <>
        {component.qualifier !== ComponentQualifier.Directory && (
          <DirectoryFacet
            branchLike={branchLike}
            directories={query.directories}
            fetching={loadingFacets.directories === true}
            open={!!openFacets.directories}
            stats={facets.directories}
            {...commonProps}
          />
        )}
        <FileFacet
          branchLike={branchLike}
          fetching={loadingFacets.files === true}
          files={query.files}
          open={!!openFacets.files}
          stats={facets.files}
          {...commonProps}
        />
      </>
    );
  }

  render() {
    const {
      appState: { settings },
      component,
      createdAfterIncludesTime,
      facets,
      openFacets,
      query,
      branchLike,
    } = this.props;

    const disableDeveloperAggregatedInfo =
      settings[GlobalSettingKeys.DeveloperAggregatedInfoDisabled] === 'true';

    const branch =
      (isBranch(branchLike) && branchLike.name) ||
      (isPullRequest(branchLike) && branchLike.branch) ||
      undefined;

    const displayPeriodFilter = component !== undefined && !isPortfolioLike(component.qualifier);
    const displayProjectsFacet = !component || isView(component.qualifier);
    const displayAuthorFacet = !component || component.qualifier !== 'DEV';

    return (
      <>
        {displayPeriodFilter && (
          <PeriodFilter
            fetching={this.props.loadingFacets.period === true}
            onChange={this.props.onFilterChange}
            stats={facets.period}
            newCodeSelected={query.inNewCodePeriod}
          />
        )}
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
        <ScopeFacet
          fetching={this.props.loadingFacets.scopes === true}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.scopes}
          stats={facets.scopes}
          scopes={query.scopes}
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
        <StandardFacet
          cwe={query.cwe}
          cweOpen={!!openFacets.cwe}
          cweStats={facets.cwe}
          fetchingCwe={this.props.loadingFacets.cwe === true}
          fetchingOwaspTop10={this.props.loadingFacets.owaspTop10 === true}
          fetchingOwaspTop10-2021={this.props.loadingFacets['owaspTop10-2021'] === true}
          fetchingSansTop25={this.props.loadingFacets.sansTop25 === true}
          fetchingSonarSourceSecurity={this.props.loadingFacets.sonarsourceSecurity === true}
          loadSearchResultCount={this.props.loadSearchResultCount}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.standards}
          owaspTop10={query.owaspTop10}
          owaspTop10Open={!!openFacets.owaspTop10}
          owaspTop10Stats={facets.owaspTop10}
          owaspTop10-2021={query['owaspTop10-2021']}
          owaspTop10-2021Open={!!openFacets['owaspTop10-2021']}
          owaspTop10-2021Stats={facets['owaspTop10-2021']}
          query={query}
          sansTop25={query.sansTop25}
          sansTop25Open={!!openFacets.sansTop25}
          sansTop25Stats={facets.sansTop25}
          sonarsourceSecurity={query.sonarsourceSecurity}
          sonarsourceSecurityOpen={!!openFacets.sonarsourceSecurity}
          sonarsourceSecurityStats={facets.sonarsourceSecurity}
        />
        <CreationDateFacet
          component={component}
          createdAfter={query.createdAfter}
          createdAfterIncludesTime={createdAfterIncludesTime}
          createdAt={query.createdAt}
          createdBefore={query.createdBefore}
          createdInLast={query.createdInLast}
          fetching={this.props.loadingFacets.createdAt === true}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.createdAt}
          inNewCodePeriod={query.inNewCodePeriod}
          stats={facets.createdAt}
        />
        <LanguageFacet
          fetching={this.props.loadingFacets.languages === true}
          loadSearchResultCount={this.props.loadSearchResultCount}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.languages}
          query={query}
          referencedLanguages={this.props.referencedLanguages}
          selectedLanguages={query.languages}
          stats={facets.languages}
        />
        <RuleFacet
          fetching={this.props.loadingFacets.rules === true}
          languages={query.languages}
          loadSearchResultCount={this.props.loadSearchResultCount}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.rules}
          query={query}
          referencedRules={this.props.referencedRules}
          rules={query.rules}
          stats={facets.rules}
        />
        <TagFacet
          component={component}
          branch={branch}
          fetching={this.props.loadingFacets.tags === true}
          loadSearchResultCount={this.props.loadSearchResultCount}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.tags}
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
            projects={query.projects}
            query={query}
            referencedComponents={this.props.referencedComponentsByKey}
            stats={facets.projects}
          />
        )}
        {this.renderComponentFacets()}
        {!this.props.myIssues && !disableDeveloperAggregatedInfo && (
          <AssigneeFacet
            assigned={query.assigned}
            assignees={query.assignees}
            fetching={this.props.loadingFacets.assignees === true}
            loadSearchResultCount={this.props.loadSearchResultCount}
            onChange={this.props.onFilterChange}
            onToggle={this.props.onFacetToggle}
            open={!!openFacets.assignees}
            query={query}
            referencedUsers={this.props.referencedUsers}
            stats={facets.assignees}
          />
        )}
        {displayAuthorFacet && !disableDeveloperAggregatedInfo && (
          <AuthorFacet
            author={query.author}
            component={component}
            fetching={this.props.loadingFacets.author === true}
            loadSearchResultCount={this.props.loadSearchResultCount}
            onChange={this.props.onFilterChange}
            onToggle={this.props.onFacetToggle}
            open={!!openFacets.author}
            query={query}
            stats={facets.author}
          />
        )}
      </>
    );
  }
}

export default withAppStateContext(Sidebar);

/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import DocLink from '../../../components/common/DocLink';
import DismissableAlert from '../../../components/ui/DismissableAlert';
import { isBranch, isPullRequest } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
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
  IssueCharacteristic,
  IssueCharacteristicFitFor,
  ReferencedComponent,
  ReferencedLanguage,
  ReferencedRule,
} from '../../../types/issues';
import { GlobalSettingKeys } from '../../../types/settings';
import { Component, Dict } from '../../../types/types';
import { UserBase } from '../../../types/users';
import { OpenFacets, Query } from '../utils';
import AssigneeFacet from './AssigneeFacet';
import AuthorFacet from './AuthorFacet';
import CharacteristicFacet from './CharacteristicFacet';
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
  facets: Dict<Facet>;
  loadSearchResultCount: (property: string, changes: Partial<Query>) => Promise<Facet>;
  loadingFacets: Dict<boolean>;
  myIssues: boolean;
  onFacetToggle: (property: string) => void;
  onFilterChange: (changes: Partial<Query>) => void;
  openFacets: OpenFacets;
  query: Query;
  referencedComponentsById: Dict<ReferencedComponent>;
  referencedComponentsByKey: Dict<ReferencedComponent>;
  referencedLanguages: Dict<ReferencedLanguage>;
  referencedRules: Dict<ReferencedRule>;
  referencedUsers: Dict<UserBase>;
  showAllFilters: boolean;
}

export class Sidebar extends React.PureComponent<Props> {
  renderComponentFacets() {
    const {
      component,
      facets,
      loadingFacets,
      openFacets,
      query,
      branchLike,
      showAllFilters,
      loadSearchResultCount,
    } = this.props;
    const hasFileOrDirectory =
      !isApplication(component?.qualifier) && !isPortfolioLike(component?.qualifier);
    if (!component || !hasFileOrDirectory) {
      return null;
    }
    const commonProps = {
      componentKey: component.key,
      loadSearchResultCount,
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
            forceShow={showAllFilters}
            {...commonProps}
          />
        )}
        <FileFacet
          branchLike={branchLike}
          fetching={loadingFacets.files === true}
          files={query.files}
          open={!!openFacets.files}
          stats={facets.files}
          forceShow={showAllFilters}
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
      showAllFilters,
      loadingFacets,
      loadSearchResultCount,
      referencedRules,
      referencedLanguages,
      referencedComponentsByKey,
      referencedUsers,
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
            fetching={loadingFacets.period === true}
            onChange={this.props.onFilterChange}
            stats={facets.period}
            newCodeSelected={query.inNewCodePeriod}
          />
        )}
        <DismissableAlert alertKey="issues-characteristic-facets" variant="info" display="inline">
          <strong>{translate('issues.characteristic.facet-highlight.title')}</strong>
          <br />
          <DocLink to="/what-sonarqube-can-do">{translate('learn_more')}</DocLink>
        </DismissableAlert>
        <CharacteristicFacet
          fetching={loadingFacets.characteristics === true}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={openFacets.characteristics?.[IssueCharacteristicFitFor.Production]}
          stats={facets.characteristics}
          fitFor={IssueCharacteristicFitFor.Production}
          characteristics={query.characteristics as IssueCharacteristic[]}
        />
        <CharacteristicFacet
          fetching={this.props.loadingFacets.characteristics === true}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={openFacets.characteristics?.[IssueCharacteristicFitFor.Development]}
          stats={facets.characteristics}
          fitFor={IssueCharacteristicFitFor.Development}
          characteristics={query.characteristics as IssueCharacteristic[]}
        />
        <SeverityFacet
          fetching={loadingFacets.severities === true}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.severities}
          severities={query.severities}
          stats={facets.severities}
        />
        <TypeFacet
          fetching={loadingFacets.types === true}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.types}
          stats={facets.types}
          types={query.types}
          forceShow={showAllFilters}
        />

        <ScopeFacet
          fetching={loadingFacets.scopes === true}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.scopes}
          stats={facets.scopes}
          scopes={query.scopes}
          forceShow={showAllFilters}
        />

        <ResolutionFacet
          fetching={loadingFacets.resolutions === true}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.resolutions}
          resolutions={query.resolutions}
          resolved={query.resolved}
          stats={facets.resolutions}
          forceShow={showAllFilters}
        />
        <StatusFacet
          fetching={loadingFacets.statuses === true}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.statuses}
          stats={facets.statuses}
          statuses={query.statuses}
          forceShow={showAllFilters}
        />
        <StandardFacet
          cwe={query.cwe}
          cweOpen={!!openFacets.cwe}
          cweStats={facets.cwe}
          fetchingCwe={loadingFacets.cwe === true}
          fetchingOwaspTop10={loadingFacets.owaspTop10 === true}
          fetchingOwaspTop10-2021={loadingFacets['owaspTop10-2021'] === true}
          fetchingSonarSourceSecurity={loadingFacets.sonarsourceSecurity === true}
          loadSearchResultCount={loadSearchResultCount}
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
          sonarsourceSecurity={query.sonarsourceSecurity}
          sonarsourceSecurityOpen={!!openFacets.sonarsourceSecurity}
          sonarsourceSecurityStats={facets.sonarsourceSecurity}
          forceShow={showAllFilters}
        />
        <CreationDateFacet
          component={component}
          createdAfter={query.createdAfter}
          createdAfterIncludesTime={createdAfterIncludesTime}
          createdAt={query.createdAt}
          createdBefore={query.createdBefore}
          createdInLast={query.createdInLast}
          fetching={loadingFacets.createdAt === true}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.createdAt}
          inNewCodePeriod={query.inNewCodePeriod}
          stats={facets.createdAt}
          forceShow={showAllFilters}
        />
        <LanguageFacet
          fetching={loadingFacets.languages === true}
          loadSearchResultCount={loadSearchResultCount}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.languages}
          query={query}
          referencedLanguages={referencedLanguages}
          selectedLanguages={query.languages}
          stats={facets.languages}
          forceShow={showAllFilters}
        />
        <RuleFacet
          fetching={loadingFacets.rules === true}
          loadSearchResultCount={loadSearchResultCount}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.rules}
          query={query}
          referencedRules={referencedRules}
          stats={facets.rules}
          forceShow={showAllFilters}
        />
        <TagFacet
          component={component}
          branch={branch}
          fetching={loadingFacets.tags === true}
          loadSearchResultCount={loadSearchResultCount}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.tags}
          query={query}
          stats={facets.tags}
          tags={query.tags}
          forceShow={showAllFilters}
        />
        {displayProjectsFacet && (
          <ProjectFacet
            component={component}
            fetching={loadingFacets.projects === true}
            loadSearchResultCount={loadSearchResultCount}
            onChange={this.props.onFilterChange}
            onToggle={this.props.onFacetToggle}
            open={!!openFacets.projects}
            projects={query.projects}
            query={query}
            referencedComponents={referencedComponentsByKey}
            stats={facets.projects}
            forceShow={showAllFilters}
          />
        )}
        {this.renderComponentFacets()}
        {!this.props.myIssues && !disableDeveloperAggregatedInfo && (
          <AssigneeFacet
            assigned={query.assigned}
            assignees={query.assignees}
            fetching={loadingFacets.assignees === true}
            loadSearchResultCount={loadSearchResultCount}
            onChange={this.props.onFilterChange}
            onToggle={this.props.onFacetToggle}
            open={!!openFacets.assignees}
            query={query}
            referencedUsers={referencedUsers}
            stats={facets.assignees}
            forceShow={showAllFilters}
          />
        )}
        {displayAuthorFacet && !disableDeveloperAggregatedInfo && (
          <AuthorFacet
            author={query.author}
            component={component}
            fetching={loadingFacets.author === true}
            loadSearchResultCount={loadSearchResultCount}
            onChange={this.props.onFilterChange}
            onToggle={this.props.onFacetToggle}
            open={!!openFacets.author}
            query={query}
            stats={facets.author}
            forceShow={showAllFilters}
          />
        )}
      </>
    );
  }
}

export default withAppStateContext(Sidebar);

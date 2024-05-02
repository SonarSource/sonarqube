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
import { BasicSeparator, FlagMessage, Link } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { isBranch, isPullRequest } from '~sonar-aligned/helpers/branch-like';
import { isPortfolioLike } from '~sonar-aligned/helpers/component';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import { translate } from '../../../helpers/l10n';
import { AppState } from '../../../types/appstate';
import { BranchLike } from '../../../types/branch-like';
import { isApplication, isProject, isView } from '../../../types/component';
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
import { AssigneeFacet } from './AssigneeFacet';
import { AttributeCategoryFacet } from './AttributeCategoryFacet';
import { AuthorFacet } from './AuthorFacet';
import { CreationDateFacet } from './CreationDateFacet';
import { DirectoryFacet } from './DirectoryFacet';
import { FileFacet } from './FileFacet';
import { IssueStatusFacet } from './IssueStatusFacet';
import { LanguageFacet } from './LanguageFacet';
import { PeriodFilter } from './PeriodFilter';
import { ProjectFacet } from './ProjectFacet';
import { RuleFacet } from './RuleFacet';
import { ScopeFacet } from './ScopeFacet';
import { SeverityFacet } from './SeverityFacet';
import { SoftwareQualityFacet } from './SoftwareQualityFacet';
import { StandardFacet } from './StandardFacet';
import { TagFacet } from './TagFacet';
import { TypeFacet } from './TypeFacet';
import { VariantFacet } from './VariantFacet';

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
  showVariantsFilter: boolean;
  query: Query;
  referencedComponentsById: Dict<ReferencedComponent>;
  referencedComponentsByKey: Dict<ReferencedComponent>;
  referencedLanguages: Dict<ReferencedLanguage>;
  referencedRules: Dict<ReferencedRule>;
  referencedUsers: Dict<UserBase>;
}

export class SidebarClass extends React.PureComponent<Props> {
  renderComponentFacets() {
    const { component, facets, loadingFacets, openFacets, query, branchLike, showVariantsFilter } =
      this.props;

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
        {showVariantsFilter && isProject(component?.qualifier) && (
          <>
            <BasicSeparator className="sw-my-4" />

            <VariantFacet
              fetching={loadingFacets.codeVariants === true}
              open={!!openFacets.codeVariants}
              stats={facets.codeVariants}
              values={query.codeVariants}
              {...commonProps}
            />
          </>
        )}

        {component.qualifier !== ComponentQualifier.Directory && (
          <>
            <BasicSeparator className="sw-my-4" />

            <DirectoryFacet
              branchLike={branchLike}
              directories={query.directories}
              fetching={loadingFacets.directories === true}
              open={!!openFacets.directories}
              stats={facets.directories}
              {...commonProps}
            />
          </>
        )}

        <BasicSeparator className="sw-my-4" />

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

    const needIssueSync = component?.needIssueSync;

    return (
      <>
        {displayPeriodFilter && (
          <PeriodFilter
            onChange={this.props.onFilterChange}
            newCodeSelected={query.inNewCodePeriod}
          />
        )}

        {!needIssueSync && (
          <>
            <AttributeCategoryFacet
              fetching={this.props.loadingFacets.cleanCodeAttributeCategories === true}
              needIssueSync={needIssueSync}
              onChange={this.props.onFilterChange}
              onToggle={this.props.onFacetToggle}
              open={!!openFacets.cleanCodeAttributeCategories}
              stats={facets.cleanCodeAttributeCategories}
              categories={query.cleanCodeAttributeCategories}
            />

            <BasicSeparator className="sw-my-4" />

            <SoftwareQualityFacet
              fetching={this.props.loadingFacets.impactSoftwareQualities === true}
              needIssueSync={needIssueSync}
              onChange={this.props.onFilterChange}
              onToggle={this.props.onFacetToggle}
              open={!!openFacets.impactSoftwareQualities}
              stats={facets.impactSoftwareQualities}
              qualities={query.impactSoftwareQualities}
            />

            <BasicSeparator className="sw-my-4" />

            <SeverityFacet
              fetching={this.props.loadingFacets.impactSeverities === true}
              onChange={this.props.onFilterChange}
              onToggle={this.props.onFacetToggle}
              open={!!openFacets.impactSeverities}
              severities={query.impactSeverities}
              stats={facets.impactSeverities}
            />

            <BasicSeparator className="sw-my-4" />
          </>
        )}

        <TypeFacet
          fetching={this.props.loadingFacets.types === true}
          needIssueSync={needIssueSync}
          onChange={this.props.onFilterChange}
          onToggle={this.props.onFacetToggle}
          open={!!openFacets.types}
          stats={facets.types}
          types={query.types}
        />
        {!needIssueSync && (
          <>
            <BasicSeparator className="sw-my-4" />

            <ScopeFacet
              fetching={this.props.loadingFacets.scopes === true}
              onChange={this.props.onFilterChange}
              onToggle={this.props.onFacetToggle}
              open={!!openFacets.scopes}
              stats={facets.scopes}
              scopes={query.scopes}
            />

            <BasicSeparator className="sw-my-4" />

            <IssueStatusFacet
              fetching={this.props.loadingFacets.issueStatuses === true}
              onChange={this.props.onFilterChange}
              onToggle={this.props.onFacetToggle}
              open={!!openFacets.issueStatuses}
              issueStatuses={query.issueStatuses}
              stats={facets.issueStatuses}
            />

            <BasicSeparator className="sw-my-4" />

            <StandardFacet
              cwe={query.cwe}
              cweOpen={!!openFacets.cwe}
              cweStats={facets.cwe}
              fetchingCwe={this.props.loadingFacets.cwe === true}
              fetchingOwaspTop10={this.props.loadingFacets.owaspTop10 === true}
              fetchingOwaspTop10-2021={this.props.loadingFacets['owaspTop10-2021'] === true}
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
              sonarsourceSecurity={query.sonarsourceSecurity}
              sonarsourceSecurityOpen={!!openFacets.sonarsourceSecurity}
              sonarsourceSecurityStats={facets.sonarsourceSecurity}
            />

            <BasicSeparator className="sw-my-4" />

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

            <BasicSeparator className="sw-my-4" />

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

            <BasicSeparator className="sw-my-4" />

            <RuleFacet
              fetching={this.props.loadingFacets.rules === true}
              loadSearchResultCount={this.props.loadSearchResultCount}
              onChange={this.props.onFilterChange}
              onToggle={this.props.onFacetToggle}
              open={!!openFacets.rules}
              query={query}
              referencedRules={this.props.referencedRules}
              stats={facets.rules}
            />

            {!disableDeveloperAggregatedInfo && (
              <>
                <BasicSeparator className="sw-my-4" />

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
                  <>
                    <BasicSeparator className="sw-my-4" />

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
                  </>
                )}

                {this.renderComponentFacets()}

                {!this.props.myIssues && (
                  <>
                    <BasicSeparator className="sw-my-4" />

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
                  </>
                )}

                <BasicSeparator className="sw-my-4" />

                <div className="sw-mb-4">
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
                </div>
              </>
            )}
          </>
        )}

        {needIssueSync && (
          <>
            <BasicSeparator className="sw-my-4" />

            <FlagMessage className="sw-my-6" variant="info">
              <div>
                {translate('indexation.page_unavailable.description')}
                <span className="sw-ml-1">
                  <FormattedMessage
                    defaultMessage={translate('indexation.filters_unavailable')}
                    id="indexation.filters_unavailable"
                    values={{
                      link: (
                        <Link to="https://docs.sonarsource.com/sonarqube/latest/instance-administration/reindexing/">
                          {translate('learn_more')}
                        </Link>
                      ),
                    }}
                  />
                </span>
              </div>
            </FlagMessage>
          </>
        )}
      </>
    );
  }
}

export const Sidebar = withAppStateContext(SidebarClass);

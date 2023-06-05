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
import classNames from 'classnames';
import { FlagMessage, HelperHintIcon, LargeCenteredLayout } from 'design-system';
import { intersection } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import A11ySkipTarget from '../../../components/a11y/A11ySkipTarget';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import ListFooter from '../../../components/controls/ListFooter';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import { Location } from '../../../components/hoc/withRouter';
import { translate } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { isApplication, isPortfolioLike } from '../../../types/component';
import { Breadcrumb, Component, ComponentMeasure, Dict, Issue, Metric } from '../../../types/types';
import '../code.css';
import { getCodeMetrics } from '../utils';
import CodeBreadcrumbs from './CodeBreadcrumbs';
import Components from './Components';
import Search from './Search';
import SearchResults from './SearchResults';
import SourceViewerWrapper from './SourceViewerWrapper';

interface Props {
  branchLike?: BranchLike;
  component: Component;
  location: Location;
  metrics: Dict<Metric>;
  baseComponent?: ComponentMeasure;
  breadcrumbs: Breadcrumb[];
  components?: ComponentMeasure[];
  highlighted?: ComponentMeasure;
  loading: boolean;
  searchResults?: ComponentMeasure[];
  sourceViewer?: ComponentMeasure;
  total: number;
  newCodeSelected: boolean;

  handleGoToParent: () => void;
  handleHighlight: (highlighted: ComponentMeasure) => void;
  handleIssueChange: (issue: Issue) => void;
  handleLoadMore: () => void;
  handleSearchClear: () => void;
  handleSearchResults: (searchResults: ComponentMeasure[]) => void;
  handleSelect: (component: ComponentMeasure) => void;
  handleSelectNewCode: (newCodeSelected: boolean) => void;
}

export default function CodeAppRenderer(props: Props) {
  const {
    branchLike,
    component,
    location,
    baseComponent,
    breadcrumbs,
    components = [],
    highlighted,
    loading,
    metrics,
    newCodeSelected,
    total,
    searchResults,
    sourceViewer,
  } = props;
  const { canBrowseAllChildProjects, qualifier } = component;

  const showSearch = searchResults !== undefined;

  const hasComponents = components.length > 0 || searchResults !== undefined;

  const showBreadcrumbs = breadcrumbs.length > 1 && !showSearch;

  const showComponentList = sourceViewer === undefined && components.length > 0 && !showSearch;

  const componentsClassName = classNames('boxed-group', 'spacer-top', {
    'new-loading': loading,
    'search-results': showSearch,
  });

  const metricKeys = intersection(
    getCodeMetrics(component.qualifier, branchLike, { newCode: newCodeSelected }),
    Object.keys(metrics)
  );
  const filteredMetrics = metricKeys.map((metric) => metrics[metric]);

  let defaultTitle = translate('code.page');
  if (isApplication(baseComponent?.qualifier)) {
    defaultTitle = translate('projects.page');
  } else if (isPortfolioLike(baseComponent?.qualifier)) {
    defaultTitle = translate('portfolio_breakdown.page');
  }

  const isPortfolio = isPortfolioLike(qualifier);

  return (
    <LargeCenteredLayout className="sw-py-8 sw-body-md">
      <Suggestions suggestions="code" />
      <Helmet defer={false} title={sourceViewer !== undefined ? sourceViewer.name : defaultTitle} />

      <A11ySkipTarget anchor="code_main" />

      {!canBrowseAllChildProjects && isPortfolio && (
        <FlagMessage
          ariaLabel={translate('code_viewer.not_all_measures_are_shown')}
          variant="warning"
          className="it__portfolio_warning sw-mb-4"
        >
          {translate('code_viewer.not_all_measures_are_shown')}
          <HelpTooltip
            className="sw-ml-2"
            overlay={translate('code_viewer.not_all_measures_are_shown.help')}
          >
            <HelperHintIcon />
          </HelpTooltip>
        </FlagMessage>
      )}

      {hasComponents && (
        <Search
          branchLike={branchLike}
          className="sw-mb-4"
          component={component}
          newCodeSelected={newCodeSelected}
          onNewCodeToggle={props.handleSelectNewCode}
          onSearchClear={props.handleSearchClear}
          onSearchResults={props.handleSearchResults}
        />
      )}

      <div className="code-components">
        {!hasComponents && sourceViewer === undefined && (
          <div className="display-flex-center display-flex-column no-file">
            <span className="h1 text-muted">
              {translate(
                'code_viewer.no_source_code_displayed_due_to_empty_analysis',
                component.qualifier
              )}
            </span>
          </div>
        )}

        {showBreadcrumbs && (
          <CodeBreadcrumbs
            branchLike={branchLike}
            breadcrumbs={breadcrumbs}
            rootComponent={component}
          />
        )}

        <div className={componentsClassName}>
          {showComponentList && (
            <Components
              baseComponent={baseComponent}
              branchLike={branchLike}
              components={components}
              cycle
              metrics={filteredMetrics}
              onEndOfList={props.handleLoadMore}
              onGoToParent={props.handleGoToParent}
              onHighlight={props.handleHighlight}
              onSelect={props.handleSelect}
              rootComponent={component}
              selected={highlighted}
              newCodeSelected={newCodeSelected}
              showAnalysisDate={isPortfolio}
            />
          )}

          {showSearch && (
            <SearchResults
              branchLike={branchLike}
              components={searchResults}
              onHighlight={props.handleHighlight}
              onSelect={props.handleSelect}
              rootComponent={component}
              selected={highlighted}
            />
          )}

          <div role="status" className={showSearch ? 'text-center big-padded-bottom' : undefined}>
            {searchResults?.length === 0 && translate('no_results')}
          </div>
        </div>

        {showComponentList && (
          <ListFooter count={components.length} loadMore={props.handleLoadMore} total={total} />
        )}

        {sourceViewer !== undefined && !showSearch && (
          <div className="spacer-top">
            <SourceViewerWrapper
              branchLike={branchLike}
              component={sourceViewer.key}
              componentMeasures={sourceViewer.measures}
              isFile
              location={location}
              onGoToParent={props.handleGoToParent}
              onIssueChange={props.handleIssueChange}
            />
          </div>
        )}
      </div>
    </LargeCenteredLayout>
  );
}

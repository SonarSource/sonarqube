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
import { Spinner } from '@sonarsource/echoes-react';
import {
  Card,
  FlagMessage,
  HelperHintIcon,
  KeyboardHint,
  LargeCenteredLayout,
  LightLabel,
} from 'design-system';
import { difference, intersection } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import A11ySkipTarget from '~sonar-aligned/components/a11y/A11ySkipTarget';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import { Location } from '~sonar-aligned/types/router';
import ListFooter from '../../../components/controls/ListFooter';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import AnalysisMissingInfoMessage from '../../../components/shared/AnalysisMissingInfoMessage';
import { CCT_SOFTWARE_QUALITY_METRICS, OLD_TAXONOMY_METRICS } from '../../../helpers/constants';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { translate } from '../../../helpers/l10n';
import { areCCTMeasuresComputed } from '../../../helpers/measures';
import { BranchLike } from '../../../types/branch-like';
import { isApplication, isPortfolioLike } from '../../../types/component';
import { Breadcrumb, Component, ComponentMeasure, Dict, Metric } from '../../../types/types';
import { getCodeMetrics } from '../utils';
import CodeBreadcrumbs from './CodeBreadcrumbs';
import Components from './Components';
import Search from './Search';
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
  handleLoadMore: () => void;
  handleSearchClear: () => void;
  handleSearchResults: (searchResults: ComponentMeasure[]) => void;
  handleSelect: (component: ComponentMeasure) => void;
  handleSelectNewCode: (newCodeSelected: boolean) => void;
}

export default function CodeAppRenderer(props: Readonly<Props>) {
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

  const metricKeys = intersection(
    getCodeMetrics(component.qualifier, branchLike, { newCode: newCodeSelected }),
    Object.keys(metrics),
  );

  const allComponentsHaveSoftwareQualityMeasures = components.every((component) =>
    areCCTMeasuresComputed(component.measures),
  );

  const filteredMetrics = difference(
    metricKeys,
    allComponentsHaveSoftwareQualityMeasures ? OLD_TAXONOMY_METRICS : CCT_SOFTWARE_QUALITY_METRICS,
  ).map((key) => metrics[key]);

  let defaultTitle = translate('code.page');
  if (isApplication(baseComponent?.qualifier)) {
    defaultTitle = translate('projects.page');
  } else if (isPortfolioLike(baseComponent?.qualifier)) {
    defaultTitle = translate('portfolio_breakdown.page');
  }

  const isPortfolio = isPortfolioLike(qualifier);

  return (
    <LargeCenteredLayout className="sw-py-8 sw-body-md" id="code-page">
      <Suggestions suggestions="code" />
      <Helmet defer={false} title={sourceViewer !== undefined ? sourceViewer.name : defaultTitle} />

      <A11ySkipTarget anchor="code_main" />

      {!canBrowseAllChildProjects && isPortfolio && (
        <FlagMessage variant="warning" className="it__portfolio_warning sw-mb-4">
          {translate('code_viewer.not_all_measures_are_shown')}
          <HelpTooltip
            className="sw-ml-2"
            overlay={translate('code_viewer.not_all_measures_are_shown.help')}
          >
            <HelperHintIcon />
          </HelpTooltip>
        </FlagMessage>
      )}

      {!allComponentsHaveSoftwareQualityMeasures && (
        <AnalysisMissingInfoMessage
          qualifier={component.qualifier}
          hide={isPortfolio}
          className="sw-mb-4"
        />
      )}

      <div className="sw-flex sw-justify-between">
        <div>
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

          {!hasComponents && sourceViewer === undefined && (
            <div className="sw-flex sw-align-center sw-flex-col sw-fixed sw-top-1/2">
              <LightLabel>
                {translate(
                  'code_viewer.no_source_code_displayed_due_to_empty_analysis',
                  component.qualifier,
                )}
              </LightLabel>
            </div>
          )}

          {showBreadcrumbs && (
            <CodeBreadcrumbs
              branchLike={branchLike}
              breadcrumbs={breadcrumbs}
              rootComponent={component}
            />
          )}
        </div>

        {(showComponentList || showSearch) && (
          <div className="sw-flex sw-items-end sw-body-sm">
            <KeyboardHint
              className="sw-mr-4 sw-ml-6"
              command={`${KeyboardKeys.DownArrow} ${KeyboardKeys.UpArrow}`}
              title={translate('component_measures.select_files')}
            />

            <KeyboardHint
              command={`${KeyboardKeys.LeftArrow} ${KeyboardKeys.RightArrow}`}
              title={translate('component_measures.navigate')}
            />
          </div>
        )}
      </div>

      {(showComponentList || showSearch) && (
        <Card className="sw-mt-2 sw-overflow-auto">
          <Spinner isLoading={loading}>
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
              <Components
                branchLike={branchLike}
                components={searchResults}
                metrics={[]}
                onHighlight={props.handleHighlight}
                onSelect={props.handleSelect}
                rootComponent={component}
                selected={highlighted}
              />
            )}
          </Spinner>
        </Card>
      )}

      {showComponentList && (
        <ListFooter count={components.length} loadMore={props.handleLoadMore} total={total} />
      )}

      {sourceViewer !== undefined && !showSearch && (
        <div className="sw-mt-2">
          <SourceViewerWrapper
            branchLike={branchLike}
            component={sourceViewer.key}
            componentMeasures={sourceViewer.measures}
            isFile
            location={location}
            onGoToParent={props.handleGoToParent}
          />
        </div>
      )}
    </LargeCenteredLayout>
  );
}

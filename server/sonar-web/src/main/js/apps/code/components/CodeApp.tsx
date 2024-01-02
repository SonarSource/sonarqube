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
import styled from '@emotion/styled';
import classNames from 'classnames';
import { debounce, intersection } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import withBranchStatusActions from '../../../app/components/branch-status/withBranchStatusActions';
import withComponentContext from '../../../app/components/componentContext/withComponentContext';
import withMetricsContext from '../../../app/components/metrics/withMetricsContext';
import A11ySkipTarget from '../../../components/a11y/A11ySkipTarget';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import ListFooter from '../../../components/controls/ListFooter';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import { Location, Router, withRouter } from '../../../components/hoc/withRouter';
import { Alert } from '../../../components/ui/Alert';
import { isPullRequest } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { CodeScope, getCodeUrl, getProjectUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { ComponentQualifier, isPortfolioLike } from '../../../types/component';
import { Breadcrumb, Component, ComponentMeasure, Dict, Issue, Metric } from '../../../types/types';
import { addComponent, addComponentBreadcrumbs, clearBucket } from '../bucket';
import '../code.css';
import {
  getCodeMetrics,
  loadMoreChildren,
  retrieveComponent,
  retrieveComponentChildren,
} from '../utils';
import Breadcrumbs from './Breadcrumbs';
import Components from './Components';
import Search from './Search';
import SourceViewerWrapper from './SourceViewerWrapper';

interface Props {
  branchLike?: BranchLike;
  component: Component;
  fetchBranchStatus: (branchLike: BranchLike, projectKey: string) => Promise<void>;
  location: Location;
  router: Router;
  metrics: Dict<Metric>;
}

interface State {
  baseComponent?: ComponentMeasure;
  breadcrumbs: Breadcrumb[];
  components?: ComponentMeasure[];
  highlighted?: ComponentMeasure;
  loading: boolean;
  page: number;
  searchResults?: ComponentMeasure[];
  sourceViewer?: ComponentMeasure;
  total: number;
  newCodeSelected: boolean;
}

export class CodeApp extends React.Component<Props, State> {
  mounted = false;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      breadcrumbs: [],
      loading: true,
      page: 0,
      total: 0,
      newCodeSelected: true,
    };
    this.refreshBranchStatus = debounce(this.refreshBranchStatus, 1000);
  }

  componentDidMount() {
    this.mounted = true;
    this.handleComponentChange();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.location.query.selected !== this.props.location.query.selected) {
      this.handleUpdate();
    }
  }

  componentWillUnmount() {
    clearBucket();
    this.mounted = false;
  }

  loadComponent = (componentKey: string) => {
    this.setState({ loading: true });
    retrieveComponent(
      componentKey,
      this.props.component.qualifier,
      this,
      this.props.branchLike
    ).then((r) => {
      if (this.mounted) {
        if (
          [ComponentQualifier.File, ComponentQualifier.TestFile].includes(
            r.component.qualifier as ComponentQualifier
          )
        ) {
          this.setState({
            breadcrumbs: r.breadcrumbs,
            components: r.components,
            loading: false,
            page: 0,
            searchResults: undefined,
            sourceViewer: r.component,
            total: 0,
          });
        } else {
          this.setState({
            baseComponent: r.component,
            breadcrumbs: r.breadcrumbs,
            components: r.components,
            loading: false,
            page: r.page,
            searchResults: undefined,
            sourceViewer: undefined,
            total: r.total,
          });
        }
      }
    }, this.stopLoading);
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  handleComponentChange = () => {
    const { branchLike, component } = this.props;

    // we already know component's breadcrumbs,
    addComponentBreadcrumbs(component.key, component.breadcrumbs);

    this.setState({ loading: true });
    retrieveComponentChildren(component.key, component.qualifier, this, branchLike).then(() => {
      addComponent(component);
      if (this.mounted) {
        this.handleUpdate();
      }
    }, this.stopLoading);
  };

  handleLoadMore = () => {
    const { baseComponent, components, page } = this.state;
    if (!baseComponent || !components) {
      return;
    }
    loadMoreChildren(
      baseComponent.key,
      page + 1,
      this.props.component.qualifier,
      this,
      this.props.branchLike
    ).then((r) => {
      if (this.mounted && r.components.length) {
        this.setState({
          components: [...components, ...r.components],
          page: r.page,
          total: r.total,
        });
      }
    }, this.stopLoading);
  };

  handleGoToParent = () => {
    const { branchLike, component } = this.props;
    const { breadcrumbs = [] } = this.state;

    if (breadcrumbs.length > 1) {
      const parentComponent = breadcrumbs[breadcrumbs.length - 2];
      this.props.router.push(getCodeUrl(component.key, branchLike, parentComponent.key));
      this.setState({ highlighted: breadcrumbs[breadcrumbs.length - 1] });
    }
  };

  handleHighlight = (highlighted: ComponentMeasure) => {
    this.setState({ highlighted });
  };

  handleIssueChange = (_: Issue) => {
    this.refreshBranchStatus();
  };

  handleSearchClear = () => {
    this.setState({ searchResults: undefined });
  };

  handleSearchResults = (searchResults: ComponentMeasure[] = []) => {
    this.setState({ searchResults });
  };

  handleSelect = (component: ComponentMeasure) => {
    const { branchLike, component: rootComponent } = this.props;
    const { newCodeSelected } = this.state;

    if (component.refKey) {
      const codeType = newCodeSelected ? CodeScope.New : CodeScope.Overall;
      const url = getProjectUrl(component.refKey, component.branch, codeType);
      this.props.router.push(url);
    } else {
      this.props.router.push(getCodeUrl(rootComponent.key, branchLike, component.key));
    }

    this.setState({ highlighted: undefined });
  };

  handleSelectNewCode = (newCodeSelected: boolean) => {
    this.setState({ newCodeSelected });
  };

  handleUpdate = () => {
    const { component, location } = this.props;
    const { selected } = location.query;
    const finalKey = selected || component.key;

    this.loadComponent(finalKey);
  };

  refreshBranchStatus = () => {
    const { branchLike, component } = this.props;
    if (branchLike && component && isPullRequest(branchLike)) {
      this.props.fetchBranchStatus(branchLike, component.key);
    }
  };

  render() {
    const { branchLike, component, location } = this.props;
    const {
      baseComponent,
      breadcrumbs,
      components = [],
      highlighted,
      loading,
      newCodeSelected,
      total,
      searchResults,
      sourceViewer,
    } = this.state;
    const { canBrowseAllChildProjects, qualifier } = component;

    const showSearch = searchResults !== undefined;

    const hasComponents = components.length > 0 || searchResults !== undefined;

    const shouldShowBreadcrumbs = breadcrumbs.length > 1 && !showSearch;

    const shouldShowComponentList =
      sourceViewer === undefined && components.length > 0 && !showSearch;

    const componentsClassName = classNames('boxed-group', 'spacer-top', {
      'new-loading': loading,
      'search-results': showSearch,
    });

    const metricKeys = intersection(
      getCodeMetrics(component.qualifier, branchLike, { newCode: newCodeSelected }),
      Object.keys(this.props.metrics)
    );
    const metrics = metricKeys.map((metric) => this.props.metrics[metric]);

    const defaultTitle =
      baseComponent &&
      [
        ComponentQualifier.Application,
        ComponentQualifier.Portfolio,
        ComponentQualifier.SubPortfolio,
      ].includes(baseComponent.qualifier as ComponentQualifier)
        ? translate('projects.page')
        : translate('code.page');

    const isPortfolio = isPortfolioLike(qualifier);

    return (
      <div className="page page-limited">
        <A11ySkipTarget anchor="code_main" />
        {!canBrowseAllChildProjects && isPortfolio && (
          <StyledAlert variant="warning" className="it__portfolio_warning">
            <AlertContent>
              {translate('code_viewer.not_all_measures_are_shown')}
              <HelpTooltip
                className="spacer-left"
                ariaLabel={translate('code_viewer.not_all_measures_are_shown.help')}
                overlay={translate('code_viewer.not_all_measures_are_shown.help')}
              />
            </AlertContent>
          </StyledAlert>
        )}
        <Suggestions suggestions="code" />
        <Helmet
          defer={false}
          title={sourceViewer !== undefined ? sourceViewer.name : defaultTitle}
        />
        {hasComponents && (
          <Search
            branchLike={branchLike}
            component={component}
            newCodeSelected={newCodeSelected}
            onNewCodeToggle={this.handleSelectNewCode}
            onSearchClear={this.handleSearchClear}
            onSearchResults={this.handleSearchResults}
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
          {shouldShowBreadcrumbs && (
            <Breadcrumbs
              branchLike={branchLike}
              breadcrumbs={breadcrumbs}
              rootComponent={component}
            />
          )}

          {shouldShowComponentList && (
            <>
              <div className={componentsClassName}>
                <Components
                  baseComponent={baseComponent}
                  branchLike={branchLike}
                  components={components}
                  cycle={true}
                  metrics={metrics}
                  onEndOfList={this.handleLoadMore}
                  onGoToParent={this.handleGoToParent}
                  onHighlight={this.handleHighlight}
                  onSelect={this.handleSelect}
                  rootComponent={component}
                  selected={highlighted}
                  newCodeSelected={newCodeSelected}
                  showAnalysisDate={isPortfolio}
                />
              </div>
              <ListFooter count={components.length} loadMore={this.handleLoadMore} total={total} />
            </>
          )}

          {showSearch && searchResults && (
            <div className={componentsClassName}>
              <Components
                branchLike={this.props.branchLike}
                components={searchResults}
                metrics={[]}
                onHighlight={this.handleHighlight}
                onSelect={this.handleSelect}
                rootComponent={component}
                selected={highlighted}
              />
            </div>
          )}

          {sourceViewer !== undefined && !showSearch && (
            <div className="spacer-top">
              <SourceViewerWrapper
                branchLike={branchLike}
                component={sourceViewer.key}
                componentMeasures={sourceViewer.measures}
                isFile={true}
                location={location}
                onGoToParent={this.handleGoToParent}
                onIssueChange={this.handleIssueChange}
              />
            </div>
          )}
        </div>
      </div>
    );
  }
}
const StyledAlert = styled(Alert)`
  display: inline-flex;
  margin-bottom: 15px;
`;

const AlertContent = styled.div`
  display: flex;
  align-items: center;
`;

export default withRouter(
  withComponentContext(withBranchStatusActions(withMetricsContext(CodeApp)))
);

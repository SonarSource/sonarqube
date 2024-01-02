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
import { debounce, keyBy } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { getMeasuresWithPeriod } from '../../../api/measures';
import { getAllMetrics } from '../../../api/metrics';
import withBranchStatusActions from '../../../app/components/branch-status/withBranchStatusActions';
import { ComponentContext } from '../../../app/components/componentContext/ComponentContext';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import { Location, Router, withRouter } from '../../../components/hoc/withRouter';
import { enhanceMeasure } from '../../../components/measure/utils';
import '../../../components/search-navigator.css';
import { Alert } from '../../../components/ui/Alert';
import { getBranchLikeQuery, isPullRequest, isSameBranchLike } from '../../../helpers/branch-like';
import {
  getLocalizedMetricDomain,
  translate,
  translateWithParameters,
} from '../../../helpers/l10n';
import {
  addSideBarClass,
  addWhitePageClass,
  removeSideBarClass,
  removeWhitePageClass,
} from '../../../helpers/pages';
import { BranchLike } from '../../../types/branch-like';
import { ComponentQualifier, isPortfolioLike } from '../../../types/component';
import {
  ComponentMeasure,
  Dict,
  Issue,
  MeasureEnhanced,
  Metric,
  Period,
} from '../../../types/types';
import Sidebar from '../sidebar/Sidebar';
import '../style.css';
import {
  banQualityGateMeasure,
  getMeasuresPageMetricKeys,
  groupByDomains,
  hasBubbleChart,
  hasFullMeasures,
  hasTree,
  hasTreemap,
  isProjectOverview,
  parseQuery,
  Query,
  serializeQuery,
  sortMeasures,
} from '../utils';
import MeasureContent from './MeasureContent';
import MeasureOverviewContainer from './MeasureOverviewContainer';
import MeasuresEmpty from './MeasuresEmpty';

interface Props {
  branchLike?: BranchLike;
  component: ComponentMeasure;
  fetchBranchStatus: (branchLike: BranchLike, projectKey: string) => Promise<void>;
  location: Location;
  router: Router;
}

interface State {
  leakPeriod?: Period;
  loading: boolean;
  measures: MeasureEnhanced[];
  metrics: Dict<Metric>;
}

export class App extends React.PureComponent<Props, State> {
  mounted = false;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      loading: true,
      measures: [],
      metrics: {},
    };
    this.refreshBranchStatus = debounce(this.refreshBranchStatus, 1000);
  }

  componentDidMount() {
    this.mounted = true;

    getAllMetrics().then(
      (metrics) => {
        const byKey = keyBy(metrics, 'key');
        this.setState({ metrics: byKey });
        this.fetchMeasures(byKey);
      },
      () => {}
    );
  }

  componentDidUpdate(prevProps: Props, prevState: State) {
    const prevQuery = parseQuery(prevProps.location.query);
    const query = parseQuery(this.props.location.query);

    if (
      !isSameBranchLike(prevProps.branchLike, this.props.branchLike) ||
      prevProps.component.key !== this.props.component.key ||
      prevQuery.selected !== query.selected
    ) {
      this.fetchMeasures(this.state.metrics);
    }

    if (prevState.measures.length === 0 && this.state.measures.length > 0) {
      addWhitePageClass();
      addSideBarClass();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    removeWhitePageClass();
    removeSideBarClass();
  }

  fetchMeasures(metrics: State['metrics']) {
    const { branchLike } = this.props;
    const query = parseQuery(this.props.location.query);
    const componentKey = query.selected || this.props.component.key;

    const filteredKeys = getMeasuresPageMetricKeys(metrics, branchLike);

    getMeasuresWithPeriod(componentKey, filteredKeys, getBranchLikeQuery(branchLike)).then(
      ({ component, period }) => {
        if (this.mounted) {
          const measures = banQualityGateMeasure(component).map((measure) =>
            enhanceMeasure(measure, metrics)
          );

          const leakPeriod =
            component.qualifier === ComponentQualifier.Project ? period : undefined;

          this.setState({
            loading: false,
            leakPeriod,
            measures: measures.filter(
              (measure) => measure.value !== undefined || measure.leak !== undefined
            ),
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  }

  getHelmetTitle = (query: Query, displayOverview: boolean, metric?: Metric) => {
    if (displayOverview && query.metric) {
      return isProjectOverview(query.metric)
        ? translate('component_measures.overview.project_overview.facet')
        : translateWithParameters(
            'component_measures.domain_x_overview',
            getLocalizedMetricDomain(query.metric)
          );
    }
    return metric ? metric.name : translate('layout.measures');
  };

  getSelectedMetric = (query: Query, displayOverview: boolean) => {
    if (displayOverview) {
      return undefined;
    }
    const metric = this.state.metrics[query.metric];
    if (!metric) {
      const domainMeasures = groupByDomains(this.state.measures);
      const firstMeasure =
        domainMeasures[0] && sortMeasures(domainMeasures[0].name, domainMeasures[0].measures)[0];
      if (firstMeasure && typeof firstMeasure !== 'string') {
        return firstMeasure.metric;
      }
    }
    return metric;
  };

  handleIssueChange = (_: Issue) => {
    this.refreshBranchStatus();
  };

  updateQuery = (newQuery: Partial<Query>) => {
    const query: Query = { ...parseQuery(this.props.location.query), ...newQuery };

    const metric = this.getSelectedMetric(query, false);
    if (metric) {
      if (query.view === 'treemap' && !hasTreemap(metric.key, metric.type)) {
        query.view = 'tree';
      } else if (query.view === 'tree' && !hasTree(metric.key)) {
        query.view = 'list';
      }
    }

    this.props.router.push({
      pathname: this.props.location.pathname,
      query: {
        ...serializeQuery(query),
        ...getBranchLikeQuery(this.props.branchLike),
        id: this.props.component.key,
      },
    });
  };

  refreshBranchStatus = () => {
    const { branchLike, component } = this.props;
    if (branchLike && component && isPullRequest(branchLike)) {
      this.props.fetchBranchStatus(branchLike, component.key);
    }
  };

  renderContent = (displayOverview: boolean, query: Query, metric?: Metric) => {
    const { branchLike, component } = this.props;
    const { leakPeriod } = this.state;
    if (displayOverview) {
      return (
        <MeasureOverviewContainer
          branchLike={branchLike}
          className="layout-page-main"
          domain={query.metric}
          leakPeriod={leakPeriod}
          metrics={this.state.metrics}
          onIssueChange={this.handleIssueChange}
          rootComponent={component}
          router={this.props.router}
          selected={query.selected}
          updateQuery={this.updateQuery}
        />
      );
    }

    if (!metric) {
      return <MeasuresEmpty />;
    }

    const hideDrilldown =
      isPullRequest(branchLike) &&
      (metric.key === 'coverage' || metric.key === 'duplicated_lines_density');

    if (hideDrilldown) {
      return (
        <div className="layout-page-main">
          <div className="layout-page-main-inner">
            <div className="note">{translate('component_measures.details_are_not_available')}</div>
          </div>
        </div>
      );
    }

    return (
      <MeasureContent
        branchLike={branchLike}
        leakPeriod={leakPeriod}
        metrics={this.state.metrics}
        onIssueChange={this.handleIssueChange}
        requestedMetric={metric}
        rootComponent={component}
        router={this.props.router}
        selected={query.selected}
        asc={query.asc}
        updateQuery={this.updateQuery}
        view={query.view}
      />
    );
  };

  render() {
    if (this.state.loading) {
      return (
        <div className="display-flex-justify-center huge-spacer-top">
          <i className="spinner" />
        </div>
      );
    }

    const { branchLike } = this.props;
    const { measures } = this.state;
    const { canBrowseAllChildProjects, qualifier } = this.props.component;
    const query = parseQuery(this.props.location.query);
    const showFullMeasures = hasFullMeasures(branchLike);
    const displayOverview = hasBubbleChart(query.metric);
    const metric = this.getSelectedMetric(query, displayOverview);

    return (
      <div id="component-measures">
        <Suggestions suggestions="component_measures" />
        <Helmet defer={false} title={this.getHelmetTitle(query, displayOverview, metric)} />
        {measures.length > 0 ? (
          <div className="layout-page">
            <ScreenPositionHelper className="layout-page-side-outer">
              {({ top }) => (
                <div className="layout-page-side" style={{ top }}>
                  <div className="layout-page-side-inner">
                    {!canBrowseAllChildProjects && isPortfolioLike(qualifier) && (
                      <Alert
                        className="big-spacer-top big-spacer-right big-spacer-left it__portfolio_warning"
                        variant="warning"
                      >
                        <AlertContent>
                          {translate('component_measures.not_all_measures_are_shown')}
                          <HelpTooltip
                            className="spacer-left"
                            ariaLabel={translate(
                              'component_measures.not_all_measures_are_shown.help'
                            )}
                            overlay={translate(
                              'component_measures.not_all_measures_are_shown.help'
                            )}
                          />
                        </AlertContent>
                      </Alert>
                    )}
                    <div className="layout-page-filters">
                      <Sidebar
                        measures={measures}
                        selectedMetric={metric ? metric.key : query.metric}
                        showFullMeasures={showFullMeasures}
                        updateQuery={this.updateQuery}
                      />
                    </div>
                  </div>
                </div>
              )}
            </ScreenPositionHelper>
            {this.renderContent(displayOverview, query, metric)}
          </div>
        ) : (
          <MeasuresEmpty />
        )}
      </div>
    );
  }
}

const AlertContent = styled.div`
  display: flex;
  align-items: center;
`;

/*
 * This needs to be refactored: the issue
 * is that we can't use the usual withComponentContext HOC, because the type
 * of `component` isn't the same. It probably used to work because of the lazy loading
 */
const WrappedApp = withRouter(withBranchStatusActions(App));

function AppWithComponentContext() {
  const { branchLike, component } = React.useContext(ComponentContext);

  return <WrappedApp branchLike={branchLike} component={component as ComponentMeasure} />;
}

export default AppWithComponentContext;

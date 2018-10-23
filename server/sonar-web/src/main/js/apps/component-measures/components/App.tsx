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
import * as key from 'keymaster';
import { InjectedRouter } from 'react-router';
import Helmet from 'react-helmet';
import MeasureContentContainer from './MeasureContentContainer';
import MeasuresEmpty from './MeasuresEmpty';
import MeasureOverviewContainer from './MeasureOverviewContainer';
import Sidebar from '../sidebar/Sidebar';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import {
  isProjectOverview,
  hasBubbleChart,
  parseQuery,
  serializeQuery,
  Query,
  hasFullMeasures,
  getMeasuresPageMetricKeys,
  groupByDomains,
  sortMeasures
} from '../utils';
import {
  isSameBranchLike,
  getBranchLikeQuery,
  isShortLivingBranch,
  isPullRequest
} from '../../../helpers/branches';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import {
  getLocalizedMetricDomain,
  translateWithParameters,
  translate
} from '../../../helpers/l10n';
import { RawQuery } from '../../../helpers/query';
import {
  BranchLike,
  ComponentMeasure,
  CurrentUser,
  MeasureEnhanced,
  Metric,
  Period
} from '../../../app/types';
import '../../../components/search-navigator.css';
import '../style.css';

interface Props {
  branchLike?: BranchLike;
  component: ComponentMeasure;
  currentUser: CurrentUser;
  location: { pathname: string; query: RawQuery };
  fetchMeasures: (
    component: string,
    metricsKey: string[],
    branchLike?: BranchLike
  ) => Promise<{ component: ComponentMeasure; measures: MeasureEnhanced[]; leakPeriod?: Period }>;
  fetchMetrics: () => void;
  metrics: { [metric: string]: Metric };
  metricsKey: string[];
  router: InjectedRouter;
}

interface State {
  loading: boolean;
  measures: MeasureEnhanced[];
  leakPeriod?: Period;
}

export default class App extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = { loading: true, measures: [] };
  }

  componentDidMount() {
    this.mounted = true;

    key.setScope('measures-files');
    this.props.fetchMetrics();
    this.fetchMeasures(this.props);
  }

  componentWillReceiveProps(nextProps: Props) {
    if (
      !isSameBranchLike(nextProps.branchLike, this.props.branchLike) ||
      nextProps.component.key !== this.props.component.key ||
      nextProps.metrics !== this.props.metrics
    ) {
      this.fetchMeasures(nextProps);
    }
  }

  componentDidUpdate(_prevProps: Props, prevState: State) {
    if (prevState.measures.length === 0 && this.state.measures.length > 0) {
      document.body.classList.add('white-page');
      if (document.documentElement) {
        document.documentElement.classList.add('white-page');
      }
      const footer = document.getElementById('footer');
      if (footer) {
        footer.classList.add('page-footer-with-sidebar');
      }
    }
  }

  componentWillUnmount() {
    this.mounted = false;

    document.body.classList.remove('white-page');
    if (document.documentElement) {
      document.documentElement.classList.remove('white-page');
    }

    const footer = document.getElementById('footer');
    if (footer) {
      footer.classList.remove('page-footer-with-sidebar');
    }

    key.deleteScope('measures-files');
  }

  fetchMeasures = ({ branchLike, component, fetchMeasures, metrics }: Props) => {
    this.setState({ loading: true });

    const filteredKeys = getMeasuresPageMetricKeys(metrics, branchLike);
    fetchMeasures(component.key, filteredKeys, branchLike).then(
      ({ measures, leakPeriod }) => {
        if (this.mounted) {
          this.setState({
            loading: false,
            leakPeriod,
            measures: measures.filter(
              measure => measure.value !== undefined || measure.leak !== undefined
            )
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

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
    const metric = this.props.metrics[query.metric];
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

  updateQuery = (newQuery: Partial<Query>) => {
    const query = serializeQuery({
      ...parseQuery(this.props.location.query),
      ...newQuery
    });
    this.props.router.push({
      pathname: this.props.location.pathname,
      query: {
        ...query,
        ...getBranchLikeQuery(this.props.branchLike),
        id: this.props.component.key
      }
    });
  };

  renderContent = (displayOverview: boolean, query: Query, metric?: Metric) => {
    const { branchLike, component, fetchMeasures, metrics } = this.props;
    const { leakPeriod } = this.state;
    if (displayOverview) {
      return (
        <MeasureOverviewContainer
          branchLike={branchLike}
          className="layout-page-main"
          currentUser={this.props.currentUser}
          domain={query.metric}
          leakPeriod={leakPeriod}
          metrics={metrics}
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
      (isShortLivingBranch(branchLike) || isPullRequest(branchLike)) &&
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
      <MeasureContentContainer
        branchLike={branchLike}
        className="layout-page-main"
        currentUser={this.props.currentUser}
        fetchMeasures={fetchMeasures}
        leakPeriod={leakPeriod}
        metric={metric}
        metrics={metrics}
        rootComponent={component}
        router={this.props.router}
        selected={query.selected}
        updateQuery={this.updateQuery}
        view={query.view}
      />
    );
  };

  render() {
    const isLoading = this.state.loading || this.props.metricsKey.length <= 0;
    if (isLoading) {
      return <i className="spinner spinner-margin" />;
    }
    const { branchLike } = this.props;
    const { measures } = this.state;
    const query = parseQuery(this.props.location.query);
    const hasOverview = hasFullMeasures(branchLike);
    const displayOverview = hasOverview && hasBubbleChart(query.metric);
    const metric = this.getSelectedMetric(query, displayOverview);
    return (
      <div id="component-measures">
        <Suggestions suggestions="component_measures" />
        <Helmet title={this.getHelmetTitle(query, displayOverview, metric)} />

        {measures.length > 0 ? (
          <div className="layout-page">
            <ScreenPositionHelper className="layout-page-side-outer">
              {({ top }) => (
                <div className="layout-page-side" style={{ top }}>
                  <div className="layout-page-side-inner">
                    <div className="layout-page-filters">
                      <Sidebar
                        hasOverview={hasOverview}
                        measures={measures}
                        selectedMetric={metric ? metric.key : query.metric}
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

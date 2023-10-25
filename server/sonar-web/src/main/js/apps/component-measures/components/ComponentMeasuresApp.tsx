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

import { withTheme } from '@emotion/react';
import styled from '@emotion/styled';
import {
  LargeCenteredLayout,
  Note,
  PageContentFontWrapper,
  Spinner,
  themeBorder,
  themeColor,
} from 'design-system';
import { keyBy } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { getMeasuresWithPeriod } from '../../../api/measures';
import { getAllMetrics } from '../../../api/metrics';
import { ComponentContext } from '../../../app/components/componentContext/ComponentContext';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import { Location, Router, withRouter } from '../../../components/hoc/withRouter';
import { enhanceMeasure } from '../../../components/measure/utils';
import '../../../components/search-navigator.css';
import { getBranchLikeQuery, isPullRequest, isSameBranchLike } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { useBranchesQuery } from '../../../queries/branch';
import { BranchLike } from '../../../types/branch-like';
import { ComponentQualifier } from '../../../types/component';
import { MeasurePageView } from '../../../types/measures';
import { MetricKey } from '../../../types/metrics';
import { ComponentMeasure, Dict, MeasureEnhanced, Metric, Period } from '../../../types/types';
import Sidebar from '../sidebar/Sidebar';
import '../style.css';
import {
  Query,
  banQualityGateMeasure,
  getMeasuresPageMetricKeys,
  groupByDomains,
  hasBubbleChart,
  hasFullMeasures,
  hasTree,
  hasTreemap,
  parseQuery,
  serializeQuery,
  sortMeasures,
} from '../utils';
import MeasureContent from './MeasureContent';
import MeasureOverviewContainer from './MeasureOverviewContainer';
import MeasuresEmpty from './MeasuresEmpty';

interface Props {
  branchLike?: BranchLike;
  component: ComponentMeasure;
  location: Location;
  router: Router;
}

interface State {
  leakPeriod?: Period;
  loading: boolean;
  measures: MeasureEnhanced[];
  metrics: Dict<Metric>;
}

class ComponentMeasuresApp extends React.PureComponent<Props, State> {
  mounted = false;
  state: State;

  constructor(props: Props) {
    super(props);

    this.state = {
      loading: true,
      measures: [],
      metrics: {},
    };
  }

  componentDidMount() {
    this.mounted = true;

    getAllMetrics().then(
      (metrics) => {
        const byKey = keyBy(metrics, 'key');
        this.setState({ metrics: byKey });
        this.fetchMeasures(byKey);
      },
      () => {},
    );
  }

  componentDidUpdate(prevProps: Props) {
    const prevQuery = parseQuery(prevProps.location.query);
    const query = parseQuery(this.props.location.query);

    if (
      // If this update is triggered by the branch query resolving, and the metrics query started
      // in componentDidMount is not done yet, we should not fetch measures just now, as it may
      // throw a bug in ITs looking for a metric that isn't there yet. In this case the measures
      // will be fetched once the state updates.
      Object.keys(this.state.metrics).length > 0 &&
      (!isSameBranchLike(prevProps.branchLike, this.props.branchLike) ||
        prevProps.component.key !== this.props.component.key ||
        prevQuery.selected !== query.selected)
    ) {
      this.fetchMeasures(this.state.metrics);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
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
            enhanceMeasure(measure, metrics),
          );

          const leakPeriod =
            component.qualifier === ComponentQualifier.Project ? period : undefined;

          this.setState({
            loading: false,
            leakPeriod,
            measures: measures.filter(
              (measure) => measure.value !== undefined || measure.leak !== undefined,
            ),
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      },
    );
  }

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

  updateQuery = (newQuery: Partial<Query>) => {
    const query: Query = { ...parseQuery(this.props.location.query), ...newQuery };

    const metric = this.getSelectedMetric(query, false);

    if (metric) {
      if (query.view === MeasurePageView.treemap && !hasTreemap(metric.key, metric.type)) {
        query.view = MeasurePageView.tree;
      } else if (query.view === MeasurePageView.tree && !hasTree(metric.key)) {
        query.view = MeasurePageView.list;
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

  renderContent = (displayOverview: boolean, query: Query, metric?: Metric) => {
    const { branchLike, component } = this.props;
    const { leakPeriod } = this.state;

    if (displayOverview) {
      return (
        <StyledMain className="sw-rounded-1 sw-mb-4">
          <MeasureOverviewContainer
            branchLike={branchLike}
            domain={query.metric}
            leakPeriod={leakPeriod}
            metrics={this.state.metrics}
            rootComponent={component}
            router={this.props.router}
            selected={query.selected}
            updateQuery={this.updateQuery}
          />
        </StyledMain>
      );
    }

    if (!metric) {
      return (
        <StyledMain className="sw-rounded-1 sw-p-6 sw-mb-4">
          <MeasuresEmpty />
        </StyledMain>
      );
    }

    const hideDrilldown =
      isPullRequest(branchLike) &&
      (metric.key === MetricKey.coverage || metric.key === MetricKey.duplicated_lines_density);

    if (hideDrilldown) {
      return (
        <StyledMain className="sw-rounded-1 sw-p-6 sw-mb-4">
          <Note>{translate('component_measures.details_are_not_available')}</Note>
        </StyledMain>
      );
    }

    return (
      <StyledMain className="sw-rounded-1 sw-mb-4">
        <MeasureContent
          asc={query.asc}
          branchLike={branchLike}
          leakPeriod={leakPeriod}
          metrics={this.state.metrics}
          requestedMetric={metric}
          rootComponent={component}
          router={this.props.router}
          selected={query.selected}
          updateQuery={this.updateQuery}
          view={query.view}
        />
      </StyledMain>
    );
  };

  render() {
    const { branchLike } = this.props;
    const { measures } = this.state;
    const { canBrowseAllChildProjects, qualifier } = this.props.component;
    const query = parseQuery(this.props.location.query);
    const showFullMeasures = hasFullMeasures(branchLike);
    const displayOverview = hasBubbleChart(query.metric);
    const metric = this.getSelectedMetric(query, displayOverview);

    return (
      <LargeCenteredLayout id="component-measures" className="sw-pt-8">
        <Suggestions suggestions="component_measures" />
        <Helmet defer={false} title={translate('layout.measures')} />
        <PageContentFontWrapper>
          <Spinner className="my-10 sw-flex sw-content-center" loading={this.state.loading} />

          {measures.length > 0 ? (
            <div className="sw-grid sw-grid-cols-12 sw-w-full">
              <Sidebar
                canBrowseAllChildProjects={!!canBrowseAllChildProjects}
                measures={measures}
                qualifier={qualifier}
                selectedMetric={metric ? metric.key : query.metric}
                showFullMeasures={showFullMeasures}
                updateQuery={this.updateQuery}
              />

              <div className="sw-col-span-9 sw-ml-12">
                {this.renderContent(displayOverview, query, metric)}
              </div>
            </div>
          ) : (
            <StyledMain className="sw-rounded-1 sw-p-6 sw-mb-4">
              <MeasuresEmpty />
            </StyledMain>
          )}
        </PageContentFontWrapper>
      </LargeCenteredLayout>
    );
  }
}

/*
 * This needs to be refactored: the issue
 * is that we can't use the usual withComponentContext HOC, because the type
 * of `component` isn't the same. It probably used to work because of the lazy loading
 */
const WrappedApp = withRouter(ComponentMeasuresApp);

function AppWithComponentContext() {
  const { component } = React.useContext(ComponentContext);
  const { data: { branchLike } = {} } = useBranchesQuery(component);

  return <WrappedApp branchLike={branchLike} component={component as ComponentMeasure} />;
}

export default AppWithComponentContext;

const StyledMain = withTheme(styled.main`
  background-color: ${themeColor('pageBlock')};
  border: ${themeBorder('default', 'pageBlockBorder')};
`);

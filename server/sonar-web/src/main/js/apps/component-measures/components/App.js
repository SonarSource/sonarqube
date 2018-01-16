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
// @flow
import React from 'react';
import Helmet from 'react-helmet';
import key from 'keymaster';
import MeasureContentContainer from './MeasureContentContainer';
import MeasureOverviewContainer from './MeasureOverviewContainer';
import Sidebar from '../sidebar/Sidebar';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import { hasBubbleChart, parseQuery, serializeQuery } from '../utils';
import { getBranchName } from '../../../helpers/branches';
import { translate } from '../../../helpers/l10n';
/*:: import type { Component, Query, Period } from '../types'; */
/*:: import type { RawQuery } from '../../../helpers/query'; */
/*:: import type { Metric } from '../../../store/metrics/actions'; */
/*:: import type { MeasureEnhanced } from '../../../components/measure/types'; */
import '../style.css';

/*:: type Props = {|
  branch?: {},
  component: Component,
  currentUser: { isLoggedIn: boolean },
  location: { pathname: string, query: RawQuery },
  fetchMeasures: (
    component: string,
    metricsKey: Array<string>,
    branch?: string
  ) => Promise<{ component: Component, measures: Array<MeasureEnhanced>, leakPeriod: ?Period }>,
  fetchMetrics: () => void,
  metrics: { [string]: Metric },
  metricsKey: Array<string>,
  router: {
    push: ({ pathname: string, query?: RawQuery }) => void
  }
|}; */

/*:: type State = {|
  loading: boolean,
  measures: Array<MeasureEnhanced>,
  leakPeriod: ?Period
|}; */

export default class App extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  /*:: state: State; */

  constructor(props /*: Props */) {
    super(props);
    this.state = {
      loading: true,
      measures: [],
      leakPeriod: null
    };
  }

  componentDidMount() {
    this.mounted = true;
    // $FlowFixMe
    document.body.classList.add('white-page');
    this.props.fetchMetrics();
    this.fetchMeasures(this.props);
    key.setScope('measures-files');
    const footer = document.getElementById('footer');
    if (footer) {
      footer.classList.add('page-footer-with-sidebar');
    }
  }

  componentWillReceiveProps(nextProps /*: Props */) {
    if (
      nextProps.branch !== this.props.branch ||
      nextProps.component.key !== this.props.component.key ||
      nextProps.metrics !== this.props.metrics
    ) {
      this.fetchMeasures(nextProps);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    // $FlowFixMe
    document.body.classList.remove('white-page');
    key.deleteScope('measures-files');
    const footer = document.getElementById('footer');
    if (footer) {
      footer.classList.remove('page-footer-with-sidebar');
    }
  }

  fetchMeasures = ({ branch, component, fetchMeasures, metrics, metricsKey } /*: Props */) => {
    this.setState({ loading: true });
    const filteredKeys = metricsKey.filter(
      key => !metrics[key].hidden && !['DATA', 'DISTRIB'].includes(metrics[key].type)
    );
    fetchMeasures(component.key, filteredKeys, getBranchName(branch)).then(
      ({ measures, leakPeriod }) => {
        if (this.mounted) {
          this.setState({
            loading: false,
            leakPeriod,
            measures: measures.filter(measure => measure.value != null || measure.leak != null)
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

  updateQuery = (newQuery /*: Query */) => {
    const query = serializeQuery({
      ...parseQuery(this.props.location.query),
      ...newQuery
    });
    this.props.router.push({
      pathname: this.props.location.pathname,
      query: {
        ...query,
        branch: getBranchName(this.props.branch),
        id: this.props.component.key
      }
    });
  };

  render() {
    const isLoading = this.state.loading || this.props.metricsKey.length <= 0;
    if (isLoading) {
      return <i className="spinner spinner-margin" />;
    }
    const { branch, component, fetchMeasures, metrics } = this.props;
    const { leakPeriod } = this.state;
    const query = parseQuery(this.props.location.query);
    const metric = metrics[query.metric];
    return (
      <div className="layout-page" id="component-measures">
        <Helmet title={translate('layout.measures')} />

        <ScreenPositionHelper className="layout-page-side-outer">
          {({ top }) => (
            <div className="layout-page-side" style={{ top }}>
              <div className="layout-page-side-inner">
                <div className="layout-page-filters">
                  <Sidebar
                    measures={this.state.measures}
                    selectedMetric={query.metric}
                    updateQuery={this.updateQuery}
                  />
                </div>
              </div>
            </div>
          )}
        </ScreenPositionHelper>

        {metric != null && (
          <MeasureContentContainer
            branch={getBranchName(branch)}
            className="layout-page-main"
            currentUser={this.props.currentUser}
            rootComponent={component}
            fetchMeasures={fetchMeasures}
            leakPeriod={leakPeriod}
            metric={metric}
            metrics={metrics}
            router={this.props.router}
            selected={query.selected}
            updateQuery={this.updateQuery}
            view={query.view}
          />
        )}
        {metric == null &&
          hasBubbleChart(query.metric) && (
            <MeasureOverviewContainer
              branch={getBranchName(branch)}
              className="layout-page-main"
              rootComponent={component}
              currentUser={this.props.currentUser}
              domain={query.metric}
              leakPeriod={leakPeriod}
              metrics={metrics}
              router={this.props.router}
              selected={query.selected}
              updateQuery={this.updateQuery}
            />
          )}
      </div>
    );
  }
}

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
import MeasureContent from './MeasureContent';
/*:: import type { Component, Period, Query } from '../types'; */
/*:: import type { MeasureEnhanced } from '../../../components/measure/types'; */
/*:: import type { Metric } from '../../../store/metrics/actions'; */
/*:: import type { RawQuery } from '../../../helpers/query'; */

/*:: type Props = {|
  branch?: string,
  className?: string,
  currentUser: { isLoggedIn: boolean },
  rootComponent: Component,
  fetchMeasures: (
    component: string,
    metricsKey: Array<string>,
    branch?: string
  ) => Promise<{ component: Component, measures: Array<MeasureEnhanced> }>,
  leakPeriod?: Period,
  metric: Metric,
  metrics: { [string]: Metric },
  router: {
    push: ({ pathname: string, query?: RawQuery }) => void
  },
  selected: ?string,
  updateQuery: Query => void,
  view: string
|}; */

/*:: type State = {
  component: ?Component,
  loading: {
    measure: boolean,
    components: boolean
  },
  measure: ?MeasureEnhanced,
  secondaryMeasure: ?MeasureEnhanced
}; */

export default class MeasureContentContainer extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  state /*: State */ = {
    component: null,
    loading: {
      measure: false,
      components: false
    },
    measure: null,
    secondaryMeasure: null
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchMeasure(this.props);
  }

  componentWillReceiveProps(nextProps /*: Props */) {
    const { component } = this.state;
    const componentChanged =
      !component ||
      nextProps.rootComponent.key !== component.key ||
      nextProps.selected !== component.key;
    if (componentChanged || nextProps.metric !== this.props.metric) {
      this.fetchMeasure(nextProps);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchMeasure = ({ branch, rootComponent, fetchMeasures, metric, selected } /*: Props */) => {
    this.updateLoading({ measure: true });

    const metricKeys = [metric.key];
    if (metric.key === 'ncloc') {
      metricKeys.push('ncloc_language_distribution');
    } else if (metric.key === 'function_complexity') {
      metricKeys.push('function_complexity_distribution');
    } else if (metric.key === 'file_complexity') {
      metricKeys.push('file_complexity_distribution');
    }

    fetchMeasures(selected || rootComponent.key, metricKeys, branch).then(
      ({ component, measures }) => {
        if (this.mounted) {
          const measure = measures.find(measure => measure.metric.key === metric.key);
          const secondaryMeasure = measures.find(measure => measure.metric.key !== metric.key);
          this.setState({ component, measure, secondaryMeasure });
          this.updateLoading({ measure: false });
        }
      },
      () => this.updateLoading({ measure: false })
    );
  };

  updateLoading = (loading /*: { [string]: boolean } */) => {
    if (this.mounted) {
      this.setState(state => ({ loading: { ...state.loading, ...loading } }));
    }
  };

  updateSelected = (component /*: string */) =>
    this.props.updateQuery({
      selected: component !== this.props.rootComponent.key ? component : null
    });

  updateView = (view /*: string */) => this.props.updateQuery({ view });

  render() {
    if (!this.state.component) {
      return null;
    }

    return (
      <MeasureContent
        branch={this.props.branch}
        className={this.props.className}
        component={this.state.component}
        currentUser={this.props.currentUser}
        loading={this.state.loading.measure || this.state.loading.components}
        leakPeriod={this.props.leakPeriod}
        measure={this.state.measure}
        metric={this.props.metric}
        metrics={this.props.metrics}
        rootComponent={this.props.rootComponent}
        router={this.props.router}
        secondaryMeasure={this.state.secondaryMeasure}
        updateLoading={this.updateLoading}
        updateSelected={this.updateSelected}
        updateView={this.updateView}
        view={this.props.view}
      />
    );
  }
}

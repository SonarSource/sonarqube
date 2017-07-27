/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import ListView from './drilldown/ListView';
import MeasureHeader from './MeasureHeader';
import MetricNotFound from './MetricNotFound';
import type { Component, Period, Query } from '../types';
import type { MeasureEnhanced } from '../../../components/measure/types';
import type { Metric } from '../../../store/metrics/actions';

type Props = {
  className?: string,
  rootComponent: Component,
  fetchMeasures: (
    Component,
    Array<string>
  ) => Promise<{ component: Component, measures: Array<MeasureEnhanced> }>,
  leakPeriod?: Period,
  metric: Metric,
  metrics: { [string]: Metric },
  selected: ?string,
  updateQuery: Query => void
};

type State = {
  component: ?Component,
  loading: {
    measure: boolean,
    components: boolean
  },
  measure: ?MeasureEnhanced,
  secondaryMeasure: ?MeasureEnhanced
};

export default class MeasureContent extends React.PureComponent {
  mounted: boolean;
  props: Props;
  state: State = {
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

  componentWillReceiveProps(nextProps: Props) {
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

  fetchMeasure = ({ rootComponent, fetchMeasures, metric, selected }: Props) => {
    this.updateLoading({ measure: true });

    const metricKeys = [metric.key];
    if (metric.key === 'ncloc') {
      metricKeys.push('ncloc_language_distribution');
    } else if (metric.key === 'function_complexity') {
      metricKeys.push('function_complexity_distribution');
    } else if (metric.key === 'file_complexity') {
      metricKeys.push('file_complexity_distribution');
    }

    fetchMeasures(selected || rootComponent.key, metricKeys).then(
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

  handleSelect = (component: Component) => this.props.updateQuery({ selected: component.key });

  updateLoading = (loading: { [string]: boolean }) => {
    if (this.mounted) {
      this.setState(state => ({ loading: { ...state.loading, ...loading } }));
    }
  };

  render() {
    const { metric } = this.props;
    const { loading, measure } = this.state;

    return (
      <div className="layout-page-main">
        <div className="layout-page-header-panel layout-page-main-header issues-main-header">
          <div className="layout-page-header-panel-inner layout-page-main-header-inner">
            <div className="layout-page-main-inner">
              Page Actions
              <DeferredSpinner
                className="pull-right"
                loading={loading.measure || loading.components}
              />
            </div>
          </div>
        </div>
        {metric != null && measure != null
          ? <div className="layout-page-main-inner">
              <MeasureHeader
                component={this.state.component}
                leakPeriod={this.props.leakPeriod}
                measure={measure}
                secondaryMeasure={this.state.secondaryMeasure}
              />
              <ListView
                component={this.state.component}
                handleSelect={this.handleSelect}
                leakPeriod={this.props.leakPeriod}
                loading={loading.components}
                metric={metric}
                metrics={this.props.metrics}
                selectedComponent={this.props.selected}
                updateLoading={this.updateLoading}
              />
            </div>
          : <MetricNotFound className="layout-page-main-inner" />}
      </div>
    );
  }
}

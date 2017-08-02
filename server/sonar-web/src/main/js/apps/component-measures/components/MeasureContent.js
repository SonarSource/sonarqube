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
import moment from 'moment';
import Breadcrumbs from './Breadcrumbs';
import Favorite from '../../../components/controls/Favorite';
import ListView from './drilldown/ListView';
import MeasureHeader from './MeasureHeader';
import MeasureViewSelect from './MeasureViewSelect';
import MetricNotFound from './MetricNotFound';
import PageActions from './PageActions';
import SourceViewer from '../../../components/SourceViewer/SourceViewer';
import { isDiffMetric } from '../../../helpers/measures';
import type { Component, Period, Query } from '../types';
import type { MeasureEnhanced } from '../../../components/measure/types';
import type { Metric } from '../../../store/metrics/actions';

type Props = {
  className?: string,
  currentUser: { isLoggedIn: boolean },
  rootComponent: Component,
  fetchMeasures: (
    Component,
    Array<string>
  ) => Promise<{ component: Component, measures: Array<MeasureEnhanced> }>,
  leakPeriod?: Period,
  metric: Metric,
  metrics: { [string]: Metric },
  selected: ?string,
  updateQuery: Query => void,
  view: string
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

  handleSelect = (component: Component) =>
    this.props.updateQuery({
      selected: component.key !== this.props.rootComponent.key ? component.key : null
    });

  updateLoading = (loading: { [string]: boolean }) => {
    if (this.mounted) {
      this.setState(state => ({ loading: { ...state.loading, ...loading } }));
    }
  };

  updateView = (view: string) => this.props.updateQuery({ view });

  renderContent() {
    const { component } = this.state;
    if (!component) {
      return null;
    }

    const { leakPeriod, metric, rootComponent, view } = this.props;
    const isFile = component.key !== rootComponent.key && component.qualifier === 'FIL';

    if (isFile) {
      const leakPeriodDate =
        isDiffMetric(metric.key) && leakPeriod != null ? moment(leakPeriod.date).toDate() : null;

      let filterLine;
      if (leakPeriodDate != null) {
        filterLine = line => {
          if (line.scmDate) {
            const scmDate = moment(line.scmDate).toDate();
            return scmDate >= leakPeriodDate;
          } else {
            return false;
          }
        };
      }
      return (
        <div className="measure-details-viewer">
          <SourceViewer component={component.key} filterLine={filterLine} />
        </div>
      );
    }

    if (view === 'list') {
      return (
        <ListView
          component={component}
          handleSelect={this.handleSelect}
          metric={metric}
          metrics={this.props.metrics}
          updateLoading={this.updateLoading}
        />
      );
    }
  }

  render() {
    const { currentUser, metric, rootComponent, view } = this.props;
    const { component, loading, measure } = this.state;
    const isLoggedIn = currentUser && currentUser.isLoggedIn;
    return (
      <div className="layout-page-main">
        <div className="layout-page-header-panel layout-page-main-header issues-main-header">
          <div className="layout-page-header-panel-inner layout-page-main-header-inner">
            <div className="layout-page-main-inner clearfix">
              {component &&
                <Breadcrumbs
                  className="measure-breadcrumbs spacer-right text-ellipsis"
                  component={component}
                  handleSelect={this.handleSelect}
                  rootComponent={rootComponent}
                  view={view}
                />}
              {component &&
                component.key !== rootComponent.key &&
                isLoggedIn &&
                <Favorite
                  favorite={component.isFavorite === true}
                  component={component.key}
                  className="measure-favorite spacer-right"
                />}
              <MeasureViewSelect
                className="measure-view-select"
                metric={this.props.metric}
                handleViewChange={this.updateView}
                view={view}
              />
              <PageActions
                loading={loading.measure || loading.components}
                isFile={component && component.qualifier === 'FIL'}
                view={view}
              />
            </div>
          </div>
        </div>
        {metric == null && <MetricNotFound className="layout-page-main-inner" />}
        {metric != null &&
          measure != null &&
          <div className="layout-page-main-inner">
            {component &&
              <MeasureHeader
                component={component}
                leakPeriod={this.props.leakPeriod}
                measure={measure}
                secondaryMeasure={this.state.secondaryMeasure}
                updateQuery={this.props.updateQuery}
              />}
            {this.renderContent()}
          </div>}
      </div>
    );
  }
}

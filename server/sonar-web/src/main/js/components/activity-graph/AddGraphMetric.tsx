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

import { Button, IconChevronDown } from '@sonarsource/echoes-react';
import { Dropdown } from 'design-system';
import { sortBy } from 'lodash';
import * as React from 'react';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import {
  CCT_SOFTWARE_QUALITY_METRICS,
  HIDDEN_METRICS,
  SOFTWARE_QUALITY_RATING_METRICS_MAP,
} from '../../helpers/constants';
import { getLocalizedMetricName, translate } from '../../helpers/l10n';
import { isDiffMetric } from '../../helpers/measures';
import { Metric } from '../../types/types';
import AddGraphMetricPopup from './AddGraphMetricPopup';

interface Props {
  metrics: Metric[];
  metricsTypeFilter?: string[];
  onAddMetric: (metric: string) => void;
  onRemoveMetric: (metric: string) => void;
  selectedMetrics: string[];
}

interface State {
  metrics: string[];
  query: string;
  selectedMetrics: string[];
}

export default class AddGraphMetric extends React.PureComponent<Props, State> {
  state: State = {
    metrics: [],
    query: '',
    selectedMetrics: [],
  };

  filterSelected = (query: string, selectedElements: string[]) => {
    return selectedElements.filter((element) =>
      this.getLocalizedMetricNameFromKey(element).toLowerCase().includes(query.toLowerCase()),
    );
  };

  filterMetricsElements = (
    { metricsTypeFilter, metrics, selectedMetrics }: Props,
    query: string,
  ) => {
    return metrics
      .filter((metric) => {
        if (metric.hidden) {
          return false;
        }
        if (isDiffMetric(metric.key)) {
          return false;
        }
        if (
          [MetricType.Data, MetricType.Distribution].includes(metric.type as MetricType) &&
          !CCT_SOFTWARE_QUALITY_METRICS.includes(metric.key as MetricKey)
        ) {
          return false;
        }
        if (HIDDEN_METRICS.includes(metric.key as MetricKey)) {
          return false;
        }
        if (Object.values(SOFTWARE_QUALITY_RATING_METRICS_MAP).includes(metric.key as MetricKey)) {
          return false;
        }
        if (
          selectedMetrics.includes(metric.key) ||
          !getLocalizedMetricName(metric).toLowerCase().includes(query.toLowerCase())
        ) {
          return false;
        }
        if (metricsTypeFilter && metricsTypeFilter.length > 0) {
          return metricsTypeFilter.includes(metric.type);
        }
        return true;
      })
      .map((metric) => metric.key);
  };

  getSelectedMetricsElements = (metrics: Metric[], selectedMetrics: string[]) => {
    return metrics
      .filter(
        (metric) =>
          selectedMetrics.includes(metric.key) &&
          !Object.values(SOFTWARE_QUALITY_RATING_METRICS_MAP).includes(metric.key as MetricKey),
      )
      .map((metric) => metric.key);
  };

  getLocalizedMetricNameFromKey = (key: string) => {
    const metric = this.props.metrics.find((m) => m.key === key);
    return metric === undefined ? key : getLocalizedMetricName(metric);
  };

  onSearch = (query: string) => {
    this.setState({ query });
    return Promise.resolve();
  };

  onSelect = (metric: string) => {
    this.props.onAddMetric(metric);
    this.setState((state) => {
      return {
        selectedMetrics: sortBy([...state.selectedMetrics, metric]),
        metrics: this.filterMetricsElements(this.props, state.query),
      };
    });
  };

  onUnselect = (metric: string) => {
    this.props.onRemoveMetric(metric);
    this.setState((state) => {
      return {
        metrics: sortBy([...state.metrics, metric]),
        selectedMetrics: state.selectedMetrics.filter((selected) => selected !== metric),
      };
    });
  };

  render() {
    const { query } = this.state;
    const filteredMetrics = this.filterMetricsElements(this.props, query);
    const selectedMetrics = this.getSelectedMetricsElements(
      this.props.metrics,
      this.props.selectedMetrics,
    );

    return (
      <Dropdown
        allowResizing
        size="large"
        closeOnClick={false}
        id="activity-graph-custom-metric-selector"
        overlay={
          <AddGraphMetricPopup
            elements={filteredMetrics}
            filterSelected={this.filterSelected}
            metricsTypeFilter={this.props.metricsTypeFilter}
            onSearch={this.onSearch}
            onSelect={this.onSelect}
            onUnselect={this.onUnselect}
            selectedElements={selectedMetrics}
          />
        }
      >
        <Button suffix={<IconChevronDown />}>
          <span className="sw-body-sm sw-flex">
            {translate('project_activity.graphs.custom.add')}
          </span>
        </Button>
      </Dropdown>
    );
  }
}

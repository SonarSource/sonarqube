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
import { sortBy } from 'lodash';
import * as React from 'react';
import { Dropdown } from '~design-system';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import { MQR_CONDITIONS_MAP, STANDARD_CONDITIONS_MAP } from '../../apps/quality-gates/utils';
import { HIDDEN_METRICS } from '../../helpers/constants';
import { getLocalizedMetricName, translate } from '../../helpers/l10n';
import { isDiffMetric } from '../../helpers/measures';
import { useStandardExperienceModeQuery } from '../../queries/mode';
import { Metric } from '../../types/types';
import AddGraphMetricPopup from './AddGraphMetricPopup';

interface Props {
  metrics: Metric[];
  metricsTypeFilter?: string[];
  onAddMetric: (metric: string) => void;
  onRemoveMetric: (metric: string) => void;
  selectedMetrics: string[];
}

export default function AddGraphMetric(props: Readonly<Props>) {
  const [metrics, setMetrics] = React.useState<string[]>([]);
  const [query, setQuery] = React.useState<string>('');
  const [selectedMetrics, setSelectedMetrics] = React.useState<string[]>([]);

  const { data: isStandardMode } = useStandardExperienceModeQuery();

  const filterSelected = (query: string, selectedElements: string[]) => {
    return selectedElements.filter((element) =>
      getLocalizedMetricNameFromKey(element).toLowerCase().includes(query.toLowerCase()),
    );
  };

  const filterMetricsElements = () => {
    const { metricsTypeFilter, metrics, selectedMetrics } = props;

    return metrics
      .filter((metric) => {
        if (metric.hidden) {
          return false;
        }
        if (isDiffMetric(metric.key)) {
          return false;
        }
        if ([MetricType.Data, MetricType.Distribution].includes(metric.type as MetricType)) {
          return false;
        }
        if (HIDDEN_METRICS.includes(metric.key as MetricKey)) {
          return false;
        }
        if (
          isStandardMode
            ? MQR_CONDITIONS_MAP[metric.key as MetricKey]
            : STANDARD_CONDITIONS_MAP[metric.key as MetricKey]
        ) {
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

  const getSelectedMetricsElements = (metrics: Metric[], selectedMetrics: string[]) => {
    return metrics
      .filter(
        (metric) =>
          selectedMetrics.includes(metric.key) &&
          (isStandardMode || !STANDARD_CONDITIONS_MAP[metric.key as MetricKey]),
      )
      .map((metric) => metric.key);
  };

  const getLocalizedMetricNameFromKey = (key: string) => {
    const metric = props.metrics.find((m) => m.key === key);
    return metric === undefined ? key : getLocalizedMetricName(metric);
  };

  const onSearch = (query: string) => {
    setQuery(query);
    return Promise.resolve();
  };

  const onSelect = (metric: string) => {
    props.onAddMetric(metric);
    setSelectedMetrics(sortBy([...selectedMetrics, metric]));
    setMetrics(filterMetricsElements());
  };

  const onUnselect = (metric: string) => {
    props.onRemoveMetric(metric);
    setSelectedMetrics(selectedMetrics.filter((selected) => selected !== metric));
    setMetrics(sortBy(metrics, metric));
  };

  const filteredMetrics = filterMetricsElements();
  const selectedElements = getSelectedMetricsElements(props.metrics, props.selectedMetrics);

  return (
    <Dropdown
      allowResizing
      size="large"
      closeOnClick={false}
      id="activity-graph-custom-metric-selector"
      overlay={
        <AddGraphMetricPopup
          elements={filteredMetrics}
          filterSelected={filterSelected}
          metricsTypeFilter={props.metricsTypeFilter}
          onSearch={onSearch}
          onSelect={onSelect}
          onUnselect={onUnselect}
          selectedElements={selectedElements}
        />
      }
    >
      <Button suffix={<IconChevronDown />}>
        <span className="sw-typo-default sw-flex">
          {translate('project_activity.graphs.custom.add')}
        </span>
      </Button>
    </Dropdown>
  );
}

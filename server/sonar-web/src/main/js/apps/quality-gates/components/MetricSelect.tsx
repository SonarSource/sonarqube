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
import { Select, SelectOption } from '@sonarsource/echoes-react';
import * as React from 'react';
import withMetricsContext from '../../../app/components/metrics/withMetricsContext';
import { translate } from '../../../helpers/l10n';
import { isDefined } from '../../../helpers/types';
import { Dict, Metric } from '../../../types/types';
import { getLocalizedMetricNameNoDiffMetric } from '../utils';

interface Props {
  metrics: Dict<Metric>;
  metricsArray: Metric[];
  onMetricChange: (metric: Metric) => void;
  selectedMetric?: Metric;
}

export function MetricSelect({
  selectedMetric,
  metricsArray,
  metrics,
  onMetricChange,
}: Readonly<Props>) {
  const handleChange = (key: string | null) => {
    if (isDefined(key)) {
      const selectedMetric = metricsArray.find((metric) => metric.key === key);
      if (selectedMetric) {
        onMetricChange(selectedMetric);
      }
    }
  };

  const options: SelectOption[] = metricsArray.map((m) => ({
    value: m.key,
    label: getLocalizedMetricNameNoDiffMetric(m, metrics),
    group: m.domain,
  }));

  return (
    <Select
      data={options}
      value={selectedMetric?.key}
      onChange={handleChange}
      ariaLabel={translate('quality_gates.conditions.fails_when')}
      isSearchable
      isNotClearable
    />
  );
}

export default withMetricsContext(MetricSelect);

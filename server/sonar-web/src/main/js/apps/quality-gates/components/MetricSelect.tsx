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
import { LabelValueSelectOption, SearchSelectDropdown } from 'design-system';
import { sortBy } from 'lodash';
import * as React from 'react';
import { Options } from 'react-select';
import withMetricsContext from '../../../app/components/metrics/withMetricsContext';
import { getLocalizedMetricDomain, translate } from '../../../helpers/l10n';
import { Dict, Metric } from '../../../types/types';
import { getLocalizedMetricNameNoDiffMetric } from '../utils';

interface Props {
  metric?: Metric;
  metricsArray: Metric[];
  metrics: Dict<Metric>;
  onMetricChange: (metric: Metric) => void;
}

interface Option {
  isDisabled?: boolean;
  label: string;
  value: string;
}

export function MetricSelect({ metric, metricsArray, metrics, onMetricChange }: Readonly<Props>) {
  const handleChange = (option: Option | null) => {
    if (option) {
      const selectedMetric = metricsArray.find((metric) => metric.key === option.value);
      if (selectedMetric) {
        onMetricChange(selectedMetric);
      }
    }
  };

  const options: Array<Option & { domain?: string }> = sortBy(
    metricsArray.map((m) => ({
      value: m.key,
      label: getLocalizedMetricNameNoDiffMetric(m, metrics),
      domain: m.domain,
    })),
    'domain',
  );

  // Use "disabled" property to emulate optgroups.
  const optionsWithDomains: Option[] = [];
  options.forEach((option, index, options) => {
    const previous = index > 0 ? options[index - 1] : null;
    if (option.domain && (!previous || previous.domain !== option.domain)) {
      optionsWithDomains.push({
        value: '<domain>',
        label: getLocalizedMetricDomain(option.domain),
        isDisabled: true,
      });
    }
    optionsWithDomains.push(option);
  });

  const handleAssigneeSearch = React.useCallback(
    (query: string, resolve: (options: Options<LabelValueSelectOption<string>>) => void) => {
      resolve(options.filter((opt) => opt.label.toLowerCase().includes(query.toLowerCase())));
    },
    [options],
  );

  return (
    <SearchSelectDropdown
      aria-label={translate('search.search_for_metrics')}
      size="large"
      controlSize="full"
      inputId="condition-metric"
      defaultOptions={optionsWithDomains}
      loadOptions={handleAssigneeSearch}
      onChange={handleChange}
      placeholder={translate('search.search_for_metrics')}
      controlLabel={
        optionsWithDomains.find((o) => o.value === metric?.key)?.label ?? translate('select_verb')
      }
    />
  );
}

export default withMetricsContext(MetricSelect);
